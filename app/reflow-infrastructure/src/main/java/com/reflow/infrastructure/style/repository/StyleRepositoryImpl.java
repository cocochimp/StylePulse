package com.reflow.infrastructure.style.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.reflow.common.lock.LevelLock;
import com.reflow.common.result.ResultDO;
import com.reflow.domain.style.model.aggregate.StyleAggregate;
import com.reflow.domain.style.model.param.StyleQuery;
import com.reflow.domain.style.repository.StyleRepository;
import com.reflow.infrastructure.lock.RedisLockFactory;
import com.reflow.infrastructure.style.converter.StyleConverter;
import com.reflow.infrastructure.style.mysql.mapper.StyleMapper;
import com.reflow.infrastructure.style.mysql.mapper.StyleSkuMapper;
import com.reflow.infrastructure.style.mysql.po.StylePO;
import com.reflow.infrastructure.style.mysql.po.StyleSkuPO;
import com.reflow.model.style.StageEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 款仓储实现（基础设施层）。
 *
 * <p>这个类是"领域接口的技术落地"：它把 {@link StyleRepository} 的语义翻译成
 * MyBatis-Plus 调用。领域层看到的是 query/save，看不到 Mapper、看不到 version 条件。</p>
 *
 * <p>乐观锁的实现方式值得注意：不是 {@code updateById}，而是
 * {@code update(entity, wrapper)}，其中 wrapper 带上"旧版本号"条件。
 * 这样 SQL 变成 {@code UPDATE ... WHERE id=? AND version=?}，
 * 并发下只有一个能成功，另一个影响行数为 0。</p>
 */
@Component
public class StyleRepositoryImpl implements StyleRepository {

    private static final Logger log = LoggerFactory.getLogger(StyleRepositoryImpl.class);

    private final StyleMapper styleMapper;
    private final StyleSkuMapper styleSkuMapper;
    private final RedisLockFactory lockFactory;

    public StyleRepositoryImpl(StyleMapper styleMapper, StyleSkuMapper styleSkuMapper, RedisLockFactory lockFactory) {
        this.styleMapper = styleMapper;
        this.styleSkuMapper = styleSkuMapper;
        this.lockFactory = lockFactory;
    }

    @Override
    public ResultDO<StyleAggregate> query(StyleQuery query) {
        try {
            if (query == null || query.getStyleId() == null) {
                return ResultDO.buildFailResult("PARAM_ERROR", "styleId 不能为空");
            }
            StylePO po = styleMapper.selectById(query.getStyleId());
            if (po == null) {
                // 查不到是正常情况，返回 success(null)，由领域服务决定怎么提示
                return ResultDO.buildSuccessResult(null);
            }
            List<StyleSkuPO> skuPOs = styleSkuMapper.selectList(
                    new LambdaQueryWrapper<StyleSkuPO>().eq(StyleSkuPO::getStyleId, po.getId()));
            return ResultDO.buildSuccessResult(StyleConverter.toAggregate(po, skuPOs));
        } catch (Throwable e) {
            log.error("查询款异常, styleId={}", query == null ? null : query.getStyleId(), e);
            return ResultDO.buildFailResult("SYSTEM_ERROR", "查询款失败");
        }
    }

    @Override
    public ResultDO<Void> save(StyleAggregate aggregate) {
        try {
            if (aggregate == null) {
                return ResultDO.buildFailResult("PARAM_ERROR", "聚合根不能为空");
            }
            StylePO po = StyleConverter.toPO(aggregate);

            if (aggregate.isNew()) {
                styleMapper.insert(po);
                aggregate.setId(po.getId());
                // 新增时把 SKU 一并写入，保证聚合落地是原子的（事务由应用层控制）
                for (StyleSkuPO skuPO : StyleConverter.toSkuPOs(aggregate)) {
                    skuPO.setStyleId(po.getId());
                    styleSkuMapper.insert(skuPO);
                }
                return ResultDO.buildSuccessResult();
            }

            // 更新：带旧版本号做 CAS。aggregate.version 已在聚合根方法里 +1，
            // 所以"旧版本" = 当前版本 - 1
            int oldVersion = (aggregate.getVersion() == null ? 1 : aggregate.getVersion()) - 1;
            int rows = styleMapper.update(po, new LambdaUpdateWrapper<StylePO>()
                    .eq(StylePO::getId, po.getId())
                    .eq(StylePO::getVersion, oldVersion));
            if (rows == 0) {
                // 影响行数为 0 有两种可能：记录不存在，或版本已被别人改掉。
                // 业务上都属于"你手上的数据过期了"，处理方式一致。
                return ResultDO.buildFailResult("VERSION_CONFLICT", "款数据已被并发修改，请重试");
            }

            for (StyleSkuPO skuPO : StyleConverter.toSkuPOs(aggregate)) {
                if (skuPO.getId() != null) {
                    styleSkuMapper.updateById(skuPO);
                }
            }
            return ResultDO.buildSuccessResult();
        } catch (Throwable e) {
            log.error("保存款异常, styleId={}", aggregate == null ? null : aggregate.getId(), e);
            return ResultDO.buildFailResult("SYSTEM_ERROR", "保存款失败");
        }
    }

    @Override
    public LevelLock buildLock(String lockKey) {
        return lockFactory.build(lockKey);
    }

    @Override
    public ResultDO<List<StyleAggregate>> queryByStages(List<StageEnum> stages, int limit) {
        try {
            List<String> stageNames = stages == null || stages.isEmpty()
                    ? new ArrayList<>()
                    : stages.stream().map(Enum::name).collect(Collectors.toList());

            LambdaQueryWrapper<StylePO> wrapper = new LambdaQueryWrapper<>();
            if (!stageNames.isEmpty()) {
                wrapper.in(StylePO::getStage, stageNames);
            }
            wrapper.orderByAsc(StylePO::getId).last("LIMIT " + Math.max(1, limit));

            List<StylePO> pos = styleMapper.selectList(wrapper);
            List<StyleAggregate> result = new ArrayList<>(pos.size());
            for (StylePO po : pos) {
                List<StyleSkuPO> skuPOs = styleSkuMapper.selectList(
                        new LambdaQueryWrapper<StyleSkuPO>().eq(StyleSkuPO::getStyleId, po.getId()));
                result.add(StyleConverter.toAggregate(po, skuPOs));
            }
            return ResultDO.buildSuccessResult(result);
        } catch (Throwable e) {
            log.error("按阶段查询款异常, stages={}", stages, e);
            return ResultDO.buildFailResult("SYSTEM_ERROR", "查询款列表失败");
        }
    }
}