package com.reflow.infrastructure.replenish.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.reflow.common.lock.LevelLock;
import com.reflow.common.result.ResultDO;
import com.reflow.domain.replenish.model.aggregate.ReplenishDecisionAggregate;
import com.reflow.domain.replenish.model.param.DecisionQuery;
import com.reflow.domain.replenish.repository.ReplenishDecisionRepository;
import com.reflow.infrastructure.lock.RedisLockFactory;
import com.reflow.infrastructure.replenish.converter.ReplenishDecisionConverter;
import com.reflow.infrastructure.replenish.mysql.mapper.ReplenishDecisionItemMapper;
import com.reflow.infrastructure.replenish.mysql.mapper.ReplenishDecisionMapper;
import com.reflow.infrastructure.replenish.mysql.po.ReplenishDecisionItemPO;
import com.reflow.infrastructure.replenish.mysql.po.ReplenishDecisionPO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 返单决策单仓储实现（承载 T3 的第三层防护）。
 *
 * <p>三个实现细节值得单独记：
 * <ol>
 *   <li><b>唯一键冲突要转成语义错误码</b>：直接把 {@link DuplicateKeyException}
 *       抛给领域层，领域层就得依赖 Spring 异常；转成 {@code DECISION_DUPLICATED}
 *       后，领域层才能按"幂等返回"处理。</li>
 *   <li><b>更新用版本 CAS</b>：{@code WHERE id=? AND version=?}，并发下只有一个生效。</li>
 *   <li><b>nextRound 必须在锁内调用</b>：两路并发都读到 max=2 就会生成两张 round=3 的单，
 *       虽然唯一键会挡住，但会导致一次无谓的失败重试。</li>
 * </ol>
 */
@Component
public class ReplenishDecisionRepositoryImpl implements ReplenishDecisionRepository {

    private static final Logger log = LoggerFactory.getLogger(ReplenishDecisionRepositoryImpl.class);

    private final ReplenishDecisionMapper decisionMapper;
    private final ReplenishDecisionItemMapper itemMapper;
    private final RedisLockFactory lockFactory;

    public ReplenishDecisionRepositoryImpl(ReplenishDecisionMapper decisionMapper,
                                           ReplenishDecisionItemMapper itemMapper,
                                           RedisLockFactory lockFactory) {
        this.decisionMapper = decisionMapper;
        this.itemMapper = itemMapper;
        this.lockFactory = lockFactory;
    }

    @Override
    public ResultDO<ReplenishDecisionAggregate> query(DecisionQuery query) {
        try {
            List<ReplenishDecisionPO> pos = selectPOs(query, 1);
            if (pos.isEmpty()) {
                return ResultDO.buildSuccessResult(null);
            }
            ReplenishDecisionPO po = pos.get(0);
            return ResultDO.buildSuccessResult(ReplenishDecisionConverter.toAggregate(po, loadItems(po.getId())));
        } catch (Throwable e) {
            log.error("查询决策单异常, query={}", query, e);
            return ResultDO.buildFailResult("SYSTEM_ERROR", "查询决策单失败");
        }
    }

    @Override
    public ResultDO<List<ReplenishDecisionAggregate>> queryList(DecisionQuery query) {
        try {
            List<ReplenishDecisionPO> pos = selectPOs(query, query == null || query.getLimit() == null ? 50 : query.getLimit());
            List<ReplenishDecisionAggregate> result = new ArrayList<>(pos.size());
            for (ReplenishDecisionPO po : pos) {
                result.add(ReplenishDecisionConverter.toAggregate(po, loadItems(po.getId())));
            }
            return ResultDO.buildSuccessResult(result);
        } catch (Throwable e) {
            log.error("查询决策单列表异常, query={}", query, e);
            return ResultDO.buildFailResult("SYSTEM_ERROR", "查询决策单列表失败");
        }
    }

    @Override
    public ResultDO<Void> save(ReplenishDecisionAggregate aggregate) {
        try {
            ReplenishDecisionPO po = ReplenishDecisionConverter.toPO(aggregate);

            if (aggregate.isNew()) {
                decisionMapper.insert(po);
                aggregate.setId(po.getId());
                for (ReplenishDecisionItemPO itemPO : ReplenishDecisionConverter.toItemPOs(aggregate)) {
                    itemPO.setDecisionId(po.getId());
                    itemMapper.insert(itemPO);
                }
                return ResultDO.buildSuccessResult();
            }

            int oldVersion = (aggregate.getVersion() == null ? 1 : aggregate.getVersion()) - 1;
            int rows = decisionMapper.update(po, new LambdaUpdateWrapper<ReplenishDecisionPO>()
                    .eq(ReplenishDecisionPO::getId, po.getId())
                    .eq(ReplenishDecisionPO::getVersion, oldVersion));
            if (rows == 0) {
                return ResultDO.buildFailResult("VERSION_CONFLICT", "决策单已被并发修改，请重试");
            }
            return ResultDO.buildSuccessResult();

        } catch (DuplicateKeyException e) {
            // uk_style_round 冲突：最后一道防线生效了，转成语义化错误码
            log.warn("决策单唯一键冲突, styleId={}, round={}",
                    aggregate == null ? null : aggregate.getStyleId(),
                    aggregate == null ? null : aggregate.getRound());
            return ResultDO.buildFailResult("DECISION_DUPLICATED", "该款本轮返单决策已存在");
        } catch (Throwable e) {
            log.error("保存决策单异常, decisionId={}", aggregate == null ? null : aggregate.getId(), e);
            return ResultDO.buildFailResult("SYSTEM_ERROR", "保存决策单失败");
        }
    }

    @Override
    public ResultDO<Integer> nextRound(Long styleId) {
        try {
            List<ReplenishDecisionPO> pos = decisionMapper.selectList(
                    new LambdaQueryWrapper<ReplenishDecisionPO>()
                            .eq(ReplenishDecisionPO::getStyleId, styleId)
                            .orderByDesc(ReplenishDecisionPO::getRound)
                            .last("LIMIT 1"));
            int next = pos.isEmpty() || pos.get(0).getRound() == null ? 1 : pos.get(0).getRound() + 1;
            return ResultDO.buildSuccessResult(next);
        } catch (Throwable e) {
            log.error("查询返单轮次异常, styleId={}", styleId, e);
            return ResultDO.buildFailResult("SYSTEM_ERROR", "查询返单轮次失败");
        }
    }

    @Override
    public LevelLock buildLock(String lockKey) {
        return lockFactory.build(lockKey);
    }

    private List<ReplenishDecisionPO> selectPOs(DecisionQuery query, int limit) {
        LambdaQueryWrapper<ReplenishDecisionPO> wrapper = new LambdaQueryWrapper<>();
        if (query != null) {
            if (query.getDecisionId() != null) {
                wrapper.eq(ReplenishDecisionPO::getId, query.getDecisionId());
            }
            if (query.getStyleId() != null) {
                wrapper.eq(ReplenishDecisionPO::getStyleId, query.getStyleId());
            }
            if (query.getStatuses() != null && !query.getStatuses().isEmpty()) {
                wrapper.in(ReplenishDecisionPO::getStatus,
                        query.getStatuses().stream().map(Enum::name).collect(Collectors.toList()));
            }
        }
        // 同款多轮时，取最近一轮
        wrapper.orderByDesc(ReplenishDecisionPO::getRound).last("LIMIT " + Math.max(1, limit));
        return decisionMapper.selectList(wrapper);
    }

    private List<ReplenishDecisionItemPO> loadItems(Long decisionId) {
        return itemMapper.selectList(new LambdaQueryWrapper<ReplenishDecisionItemPO>()
                .eq(ReplenishDecisionItemPO::getDecisionId, decisionId));
    }
}