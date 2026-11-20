package com.reflow.domain.replenish.service;

import com.reflow.common.result.ResultDO;
import com.reflow.domain.replenish.model.param.ApproveDecisionParam;
import com.reflow.domain.replenish.model.param.OpenDecisionParam;
import com.reflow.domain.replenish.model.result.ReplenishDecisionResult;

/**
 * 返单决策领域服务（写模式）。
 *
 * <p>注意它<b>不加载款聚合</b>：款的阶段校验、指标计算都在应用层编排时完成。
 * 领域服务只对自己上下文内的聚合负责——这条边界守住了，
 * 才能保证"改款的规则"永远只在 style 上下文里。</p>
 */
public interface ReplenishDomainService {

    /**
     * 开启决策单（三层防重中的锁与幂等在这一层）。
     *
     * <p>返回 {@code duplicated=true} 表示该款已有进行中的决策，本次请求被幂等吸收。</p>
     */
    ResultDO<ReplenishDecisionResult> openDecision(OpenDecisionParam param);

    /** 审批：批准或驳回 */
    ResultDO<ReplenishDecisionResult> approveDecision(ApproveDecisionParam param);

    /** 标记产能待定（预占失败时挂起等待重试） */
    ResultDO<Void> markPendingCapacity(Long decisionId);

    /** 标记已下发（生产指令送达工厂后） */
    ResultDO<Void> markDispatched(Long decisionId);

    /** 查询决策单快照 */
    ResultDO<ReplenishDecisionResult> queryDecision(Long decisionId);
}