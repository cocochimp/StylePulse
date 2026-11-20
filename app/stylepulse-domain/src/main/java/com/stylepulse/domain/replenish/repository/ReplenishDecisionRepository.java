package com.stylepulse.domain.replenish.repository;

import com.stylepulse.common.base.AggregateRepository;
import com.stylepulse.common.lock.LevelLock;
import com.stylepulse.common.result.ResultDO;
import com.stylepulse.domain.replenish.model.aggregate.ReplenishDecisionAggregate;
import com.stylepulse.domain.replenish.model.param.DecisionQuery;

import java.util.List;

/**
 * 返单决策单仓储接口。
 */
public interface ReplenishDecisionRepository extends AggregateRepository<ReplenishDecisionAggregate, Long> {

    /** 加载单个决策单；不存在返回 success(null) */
    ResultDO<ReplenishDecisionAggregate> query(DecisionQuery query);

    /** 列表查询（看板用） */
    ResultDO<List<ReplenishDecisionAggregate>> queryList(DecisionQuery query);

    /**
     * 保存决策单。
     *
     * <p>实现必须同时处理两件事：乐观锁（version CAS）和唯一键（style_id+round）。
     * 冲突时返回 {@code DECISION_DUPLICATED}，由领域服务转成"幂等返回"。</p>
     */
    ResultDO<Void> save(ReplenishDecisionAggregate aggregate);

    /** 取该款的第几次返单；锁内调用 */
    ResultDO<Integer> nextRound(Long styleId);

    LevelLock buildLock(String lockKey);
}