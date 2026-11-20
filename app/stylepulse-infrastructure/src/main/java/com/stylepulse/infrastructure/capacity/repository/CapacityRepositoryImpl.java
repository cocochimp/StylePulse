package com.stylepulse.infrastructure.capacity.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.stylepulse.common.result.ResultDO;
import com.stylepulse.domain.capacity.model.aggregate.CapacitySlotAggregate;
import com.stylepulse.domain.capacity.model.param.ReserveCapacityParam;
import com.stylepulse.domain.capacity.model.result.ReserveCapacityResult;
import com.stylepulse.domain.capacity.repository.CapacityRepository;
import com.stylepulse.infrastructure.capacity.converter.CapacityConverter;
import com.stylepulse.infrastructure.capacity.mysql.mapper.CapacityHoldMapper;
import com.stylepulse.infrastructure.capacity.mysql.mapper.CapacitySlotMapper;
import com.stylepulse.infrastructure.capacity.mysql.po.CapacityHoldPO;
import com.stylepulse.infrastructure.capacity.mysql.po.CapacitySlotPO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 产能仓储实现（T4 的核心）。
 *
 * <p>设计要点：<b>Redis 当闸门，DB 当账本</b>。</p>
 * <ul>
 *   <li>Redis 里的 {@code used} 是"准入判断依据"，要求极快、原子，允许重启后重建</li>
 *   <li>DB 里的 hold 行是"事实记录"，要求可审计、可追溯，是对账的基准</li>
 *   <li>两者不一致时，<b>以 DB 为准回写 Redis</b>——因为 hold 记录才是钱</li>
 * </ul>
 *
 * <p>为什么不用数据库行锁（{@code SELECT ... FOR UPDATE}）：
 * 产能槽是热点行，几十个决策单同时抢，行锁会把连接池占满，
 * 吞吐掉到百级；而这里只需要"读-判-写"原子，Redis 单线程执行天然满足。</p>
 */
@Component
public class CapacityRepositoryImpl implements CapacityRepository {

    private static final Logger log = LoggerFactory.getLogger(CapacityRepositoryImpl.class);

    private static final String CAP_KEY_PREFIX = "cap:";

    /** 预占存活时长：超过这个时间还没确认，认为流程已死，可被回收 */
    private static final Duration HOLD_TTL = Duration.ofMinutes(30);

    /** 槽位缓存有效期：到期后从 DB 重建，避免长期漂移 */
    private static final Duration SLOT_TTL = Duration.ofDays(2);

    private final CapacitySlotMapper slotMapper;
    private final CapacityHoldMapper holdMapper;
    private final StringRedisTemplate redis;
    private final DefaultRedisScript<Long> reserveScript;
    private final DefaultRedisScript<Long> releaseScript;

    public CapacityRepositoryImpl(CapacitySlotMapper slotMapper,
                                  CapacityHoldMapper holdMapper,
                                  StringRedisTemplate redis) {
        this.slotMapper = slotMapper;
        this.holdMapper = holdMapper;
        this.redis = redis;
        this.reserveScript = new DefaultRedisScript<>();
        // 脚本放 resources 而不是内联字符串：便于本地用 redis-cli EVAL 调试
        this.reserveScript.setLocation(new ClassPathResource("lua/capacity_reserve.lua"));
        this.reserveScript.setResultType(Long.class);
        this.releaseScript = new DefaultRedisScript<>();
        this.releaseScript.setLocation(new ClassPathResource("lua/capacity_release.lua"));
        this.releaseScript.setResultType(Long.class);
    }

    @Override
    public ResultDO<ReserveCapacityResult> tryReserve(ReserveCapacityParam param) {
        String slotKey = param.slotKey();
        String key = CAP_KEY_PREFIX + slotKey;
        try {
            if (!ensureSlot(key, slotKey)) {
                return ResultDO.buildSuccessResult(ReserveCapacityResult.rejected("SLOT_NOT_FOUND", 0));
            }

            Long result = redis.execute(reserveScript, List.of(key), String.valueOf(param.getQty()));

            // -1 表示 Redis 里没有这个槽（TTL 到期或实例重启）。重建后重试一次，
            // 只重试一次：如果重建后仍然失败，说明是配置/数据问题，重试无意义
            if (result != null && result == -1L) {
                if (!ensureSlot(key, slotKey)) {
                    return ResultDO.buildSuccessResult(ReserveCapacityResult.rejected("SLOT_NOT_FOUND", 0));
                }
                result = redis.execute(reserveScript, List.of(key), String.valueOf(param.getQty()));
            }

            if (result == null || result == -1L) {
                return ResultDO.buildFailResult("CAPACITY_UNAVAILABLE", "产能服务暂时不可用");
            }
            if (result == 0L) {
                int remain = remainQty(key);
                log.info("产能不足, slot={}, 申请={}, 剩余={}", slotKey, param.getQty(), remain);
                return ResultDO.buildSuccessResult(ReserveCapacityResult.rejected("CAPACITY_NOT_ENOUGH", remain));
            }

            // 闸门已放行，接下来记账。记账失败必须把 Redis 的量减回去，
            // 否则会留下一个"占着但不属于任何人"的幽灵占用
            String holdId = UUID.randomUUID().toString().replace("-", "");
            try {
                CapacityHoldPO hold = new CapacityHoldPO();
                hold.setHoldId(holdId);
                hold.setSlotKey(slotKey);
                hold.setQty(param.getQty());
                hold.setDecisionId(param.getDecisionId());
                hold.setStatus(CapacityHoldPO.STATUS_RESERVED);
                hold.setExpireAt(LocalDateTime.now().plusMinutes(30));
                holdMapper.insert(hold);
            } catch (Throwable e) {
                log.error("预占记账失败，回滚 Redis, slot={}, qty={}", slotKey, param.getQty(), e);
                rollbackRedis(key, param.getQty());
                return ResultDO.buildFailResult("SYSTEM_ERROR", "产能预占记账失败");
            }

            return ResultDO.buildSuccessResult(ReserveCapacityResult.ok(holdId, remainQty(key)));

        } catch (Throwable e) {
            log.error("产能预占异常, slot={}", slotKey, e);
            return ResultDO.buildFailResult("SYSTEM_ERROR", "产能预占异常");
        }
    }

    @Override
    public ResultDO<Void> confirm(String holdId) {
        try {
            CapacityHoldPO hold = selectHold(holdId);
            if (hold == null) {
                return ResultDO.buildFailResult("HOLD_NOT_FOUND", "预占凭证不存在");
            }
            if (CapacityHoldPO.STATUS_CONFIRMED.equals(hold.getStatus())) {
                // 幂等：重复确认直接成功
                return ResultDO.buildSuccessResult();
            }
            if (CapacityHoldPO.STATUS_RELEASED.equals(hold.getStatus())) {
                return ResultDO.buildFailResult("HOLD_RELEASED", "预占已释放，无法确认");
            }
            holdMapper.update(null, new LambdaUpdateWrapper<CapacityHoldPO>()
                    .eq(CapacityHoldPO::getHoldId, holdId)
                    .set(CapacityHoldPO::getStatus, CapacityHoldPO.STATUS_CONFIRMED)
                    // 确认后不再超时回收，把到期时间清空
                    .set(CapacityHoldPO::getExpireAt, null));
            return ResultDO.buildSuccessResult();
        } catch (Throwable e) {
            log.error("产能确认异常, holdId={}", holdId, e);
            return ResultDO.buildFailResult("SYSTEM_ERROR", "产能确认异常");
        }
    }

    @Override
    public ResultDO<Void> release(String holdId) {
        try {
            CapacityHoldPO hold = selectHold(holdId);
            if (hold == null) {
                return ResultDO.buildFailResult("HOLD_NOT_FOUND", "预占凭证不存在");
            }
            if (CapacityHoldPO.STATUS_RELEASED.equals(hold.getStatus())) {
                // 幂等：重复释放直接成功，不能重复扣减 Redis
                return ResultDO.buildSuccessResult();
            }

            String key = CAP_KEY_PREFIX + hold.getSlotKey();
            redis.execute(releaseScript, List.of(key), String.valueOf(hold.getQty()));

            holdMapper.update(null, new LambdaUpdateWrapper<CapacityHoldPO>()
                    .eq(CapacityHoldPO::getHoldId, holdId)
                    .set(CapacityHoldPO::getStatus, CapacityHoldPO.STATUS_RELEASED));
            return ResultDO.buildSuccessResult();
        } catch (Throwable e) {
            log.error("产能释放异常, holdId={}", holdId, e);
            return ResultDO.buildFailResult("SYSTEM_ERROR", "产能释放异常");
        }
    }

    @Override
    public ResultDO<CapacitySlotAggregate> querySlot(String slotKey) {
        try {
            CapacitySlotPO po = selectSlot(slotKey);
            if (po == null) {
                return ResultDO.buildSuccessResult(null);
            }
            return ResultDO.buildSuccessResult(CapacityConverter.toAggregate(po, usedFromDb(slotKey)));
        } catch (Throwable e) {
            log.error("查询产能槽异常, slotKey={}", slotKey, e);
            return ResultDO.buildFailResult("SYSTEM_ERROR", "查询产能槽失败");
        }
    }

    @Override
    public ResultDO<List<String>> queryExpiredHoldIds(int limit) {
        try {
            List<CapacityHoldPO> holds = holdMapper.selectList(new LambdaQueryWrapper<CapacityHoldPO>()
                    .eq(CapacityHoldPO::getStatus, CapacityHoldPO.STATUS_RESERVED)
                    .isNotNull(CapacityHoldPO::getExpireAt)
                    .lt(CapacityHoldPO::getExpireAt, LocalDateTime.now())
                    .orderByAsc(CapacityHoldPO::getId)
                    .last("LIMIT " + Math.max(1, limit)));
            List<String> ids = new ArrayList<>(holds.size());
            holds.forEach(hold -> ids.add(hold.getHoldId()));
            return ResultDO.buildSuccessResult(ids);
        } catch (Throwable e) {
            log.error("查询超时预占异常", e);
            return ResultDO.buildFailResult("SYSTEM_ERROR", "查询超时预占失败");
        }
    }

    // ------------------------------------------------------------------
    // 内部方法
    // ------------------------------------------------------------------

    /**
     * 确保 Redis 里有这个槽位。
     *
     * <p>缓存未命中时从 DB 重建，这是经典的 cache-aside。
     * 注意重建时写入的 used 来自 hold 表汇总，而不是 Redis 里的旧值——
     * 因为此刻 Redis 里本来就没有可信值。</p>
     */
    private boolean ensureSlot(String key, String slotKey) {
        Boolean exists = redis.hasKey(key);
        if (Boolean.TRUE.equals(exists)) {
            return true;
        }
        CapacitySlotPO po = selectSlot(slotKey);
        if (po == null) {
            return false;
        }
        int used = usedFromDb(slotKey);
        redis.opsForHash().put(key, "total", String.valueOf(po.getTotalQty()));
        redis.opsForHash().put(key, "used", String.valueOf(used));
        redis.expire(key, SLOT_TTL);
        log.info("产能槽从 DB 重建, slot={}, total={}, used={}", slotKey, po.getTotalQty(), used);
        return true;
    }

    private CapacitySlotPO selectSlot(String slotKey) {
        String[] parts = slotKey.split(":");
        if (parts.length != 3) {
            return null;
        }
        return slotMapper.selectOne(new LambdaQueryWrapper<CapacitySlotPO>()
                .eq(CapacitySlotPO::getFactoryId, Long.valueOf(parts[0]))
                .eq(CapacitySlotPO::getLineId, Long.valueOf(parts[1]))
                .eq(CapacitySlotPO::getBizDate, parts[2]));
    }

    /** 汇总未释放的预占量：RESERVED + CONFIRMED 都算占用 */
    private int usedFromDb(String slotKey) {
        List<CapacityHoldPO> holds = holdMapper.selectList(new LambdaQueryWrapper<CapacityHoldPO>()
                .eq(CapacityHoldPO::getSlotKey, slotKey)
                .in(CapacityHoldPO::getStatus, CapacityHoldPO.STATUS_RESERVED, CapacityHoldPO.STATUS_CONFIRMED));
        return holds.stream().mapToInt(h -> h.getQty() == null ? 0 : h.getQty()).sum();
    }

    private CapacityHoldPO selectHold(String holdId) {
        return holdMapper.selectOne(new LambdaQueryWrapper<CapacityHoldPO>()
                .eq(CapacityHoldPO::getHoldId, holdId));
    }

    private int remainQty(String key) {
        Object totalObj = redis.opsForHash().get(key, "total");
        Object usedObj = redis.opsForHash().get(key, "used");
        int total = totalObj == null ? 0 : Integer.parseInt(String.valueOf(totalObj));
        int used = usedObj == null ? 0 : Integer.parseInt(String.valueOf(usedObj));
        return Math.max(0, total - used);
    }

    private void rollbackRedis(String key, int qty) {
        try {
            redis.execute(releaseScript, List.of(key), String.valueOf(qty));
        } catch (Throwable e) {
            // 回滚失败只能告警：理论上会留下少量幽灵占用，靠定时对账修正
            log.error("回滚 Redis 占用量失败, key={}, qty={}", key, qty, e);
        }
    }
}