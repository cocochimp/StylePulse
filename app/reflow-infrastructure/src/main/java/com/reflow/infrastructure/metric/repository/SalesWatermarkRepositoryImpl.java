package com.reflow.infrastructure.metric.repository;

import com.reflow.common.result.ResultDO;
import com.reflow.domain.metric.model.aggregate.SalesWatermarkAggregate;
import com.reflow.domain.metric.model.param.SalesWindowQuery;
import com.reflow.domain.metric.repository.SalesWatermarkRepository;
import com.reflow.model.metric.WindowSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 销量水位仓储实现（Redis 分桶，T2 的存储侧）。
 *
 * <p>Key 设计：
 * <pre>
 * wm:bucket:{styleId}  HASH   field=桶起点时间戳  value=该桶销量     ← 计数主体
 * wm:exp:{styleId}     ZSET   member=桶起点      score=桶起点        ← 用于按时间区间取桶 + 淘汰
 * wm:meta:{styleId}    HASH   lastEventTime                          ← 水位线（放在同一实例，避免双写不一致）
 * evt:{eventId}        STRING 幂等标记，TTL 8 天
 * </pre>
 *
 * <p>为什么桶和索引要分成两个结构：HASH 适合"按 field 批量取值"，ZSET 适合"按 score 区间定位"。
 * 只用一个 ZSET 存每条事件的话，单款单日万级事件会让内存和范围查询都失控——
 * 桶化就是把"事件级"压成"桶级"，这是数量级的优化。</p>
 */
@Component
public class SalesWatermarkRepositoryImpl implements SalesWatermarkRepository {

    private static final Logger log = LoggerFactory.getLogger(SalesWatermarkRepositoryImpl.class);

    private static final String KEY_BUCKET = "wm:bucket:";
    private static final String KEY_EXP = "wm:exp:";
    private static final String KEY_META = "wm:meta:";
    private static final String KEY_EVENT = "evt:";

    /** 桶数据保留 8 天：比最长窗口多 1 天余量，便于迟到补偿 */
    private static final Duration BUCKET_TTL = Duration.ofDays(8);
    private static final Duration META_TTL = Duration.ofDays(30);

    private final StringRedisTemplate redis;

    public SalesWatermarkRepositoryImpl(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public ResultDO<Boolean> tryMarkEvent(String eventId) {
        try {
            Boolean ok = redis.opsForValue().setIfAbsent(
                    KEY_EVENT + eventId, "1", Duration.ofSeconds(WindowSpec.EVENT_DEDUP_TTL_SECONDS));
            return ResultDO.buildSuccessResult(Boolean.TRUE.equals(ok));
        } catch (Throwable e) {
            log.error("事件去重标记异常, eventId={}", eventId, e);
            return ResultDO.buildFailResult("SYSTEM_ERROR", "事件去重失败");
        }
    }

    @Override
    public ResultDO<Void> incrBucket(Long styleId, long bucketTs, int qty) {
        try {
            String bucketKey = KEY_BUCKET + styleId;
            String expKey = KEY_EXP + styleId;
            String field = String.valueOf(bucketTs);

            redis.opsForHash().increment(bucketKey, field, qty);
            // ZSET 用于定位"窗口内有哪些桶"，重复添加同一 member 是幂等的
            redis.opsForZSet().add(expKey, field, bucketTs);

            // TTL 只在首次写入时设置；续期由每日清理任务统一处理
            redis.expire(bucketKey, BUCKET_TTL);
            redis.expire(expKey, BUCKET_TTL);
            return ResultDO.buildSuccessResult();
        } catch (Throwable e) {
            log.error("桶累加异常, styleId={}, bucket={}", styleId, bucketTs, e);
            return ResultDO.buildFailResult("SYSTEM_ERROR", "桶累加失败");
        }
    }

    @Override
    public ResultDO<Integer> sumWindow(SalesWindowQuery query) {
        try {
            String expKey = KEY_EXP + query.getStyleId();
            // 第一步：按 score 区间取出窗口内的桶
            Set<String> buckets = redis.opsForZSet()
                    .rangeByScore(expKey, query.getFromBucket(), query.getToBucket());
            if (buckets == null || buckets.isEmpty()) {
                return ResultDO.buildSuccessResult(0);
            }
            // 第二步：批量取这些桶的计数。用 HMGET 而不是逐个 HGET，减少往返
            List<Object> fields = new ArrayList<>(buckets);
            List<Object> values = redis.opsForHash().multiGet(KEY_BUCKET + query.getStyleId(), fields);

            int total = 0;
            if (values != null) {
                for (Object value : values) {
                    if (value != null) {
                        total += Integer.parseInt(String.valueOf(value));
                    }
                }
            }
            return ResultDO.buildSuccessResult(total);
        } catch (Throwable e) {
            log.error("窗口汇总异常, query={}", query, e);
            return ResultDO.buildFailResult("SYSTEM_ERROR", "窗口汇总失败");
        }
    }

    @Override
    public ResultDO<SalesWatermarkAggregate> query(Long styleId) {
        try {
            Map<Object, Object> meta = redis.opsForHash().entries(KEY_META + styleId);
            SalesWatermarkAggregate aggregate = new SalesWatermarkAggregate(styleId);
            if (meta != null && meta.get("lastEventTime") != null) {
                aggregate.setLastEventTime(Long.parseLong(String.valueOf(meta.get("lastEventTime"))));
            }
            if (meta != null && meta.get("version") != null) {
                aggregate.setVersion(Integer.parseInt(String.valueOf(meta.get("version"))));
            }
            // 查不到也返回对象（lastEventTime=null），表示"还没有任何事件"
            return ResultDO.buildSuccessResult(aggregate);
        } catch (Throwable e) {
            log.error("加载水位异常, styleId={}", styleId, e);
            return ResultDO.buildFailResult("SYSTEM_ERROR", "加载水位失败");
        }
    }

    @Override
    public ResultDO<Void> save(SalesWatermarkAggregate aggregate) {
        try {
            Map<String, String> meta = new HashMap<>(4);
            meta.put("lastEventTime", String.valueOf(aggregate.getLastEventTime()));
            meta.put("version", String.valueOf(aggregate.getVersion()));
            String metaKey = KEY_META + aggregate.getStyleId();
            redis.opsForHash().putAll(metaKey, meta);
            redis.expire(metaKey, META_TTL);
            return ResultDO.buildSuccessResult();
        } catch (Throwable e) {
            log.error("保存水位异常, styleId={}", aggregate.getStyleId(), e);
            return ResultDO.buildFailResult("SYSTEM_ERROR", "保存水位失败");
        }
    }

    @Override
    public ResultDO<Void> markLateEvent(String eventId, Long styleId) {
        try {
            // 迟到事件量很小，直接进一个 LIST，由对账任务按 eventTime 回插
            redis.opsForList().rightPush("wm:late:" + styleId, eventId);
            return ResultDO.buildSuccessResult();
        } catch (Throwable e) {
            log.error("记录迟到事件异常, eventId={}", eventId, e);
            return ResultDO.buildFailResult("SYSTEM_ERROR", "记录迟到事件失败");
        }
    }
}