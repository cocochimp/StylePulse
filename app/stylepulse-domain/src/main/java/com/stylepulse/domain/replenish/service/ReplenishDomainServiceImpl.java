package com.stylepulse.domain.replenish.service;

import com.stylepulse.common.exception.AggregateException;
import com.stylepulse.common.exception.BizException;
import com.stylepulse.common.lock.LevelLock;
import com.stylepulse.common.result.ResultDO;
import com.stylepulse.domain.replenish.model.aggregate.ReplenishDecisionAggregate;
import com.stylepulse.domain.replenish.model.entity.ReplenishDecisionItemEntity;
import com.stylepulse.domain.replenish.model.param.ApproveDecisionParam;
import com.stylepulse.domain.replenish.model.param.DecisionQuery;
import com.stylepulse.domain.replenish.model.param.OpenDecisionParam;
import com.stylepulse.domain.replenish.model.result.ReplenishDecisionResult;
import com.stylepulse.domain.replenish.repository.ReplenishDecisionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 返单决策领域服务实现。
 *
 * <p>这个类是 T3（重复追单幂等）的第一、二层：
 * <ul>
 *   <li><b>第一层</b>：{@code buildLock("style:decide:" + styleId)} 挡住同款并发</li>
 *   <li><b>第二层</b>：查"是否已有进行中的单" + 保存时 version CAS</li>
 *   <li><b>第三层</b>：唯一键 {@code uk_style_round} 在仓储层兜底（见实现类）</li>
 * </ul>
 * 三层不是冗余，各自挡的是不同失效模式：锁超时、重复投递、实现 bug。</p>
 */
@Service
public class ReplenishDomainServiceImpl implements ReplenishDomainService {

    private static final Logger log = LoggerFactory.getLogger(ReplenishDomainServiceImpl.class);

    private final ReplenishDecisionRepository replenishDecisionRepository;

    public ReplenishDomainServiceImpl(ReplenishDecisionRepository replenishDecisionRepository) {
        this.replenishDecisionRepository = replenishDecisionRepository;
    }

    @Override
    public ResultDO<ReplenishDecisionResult> openDecision(OpenDecisionParam param) {
        if (param == null || param.getStyleId() == null) {
            return ResultDO.buildFailResult("PARAM_ERROR", "styleId 不能为空");
        }
        LevelLock lock = replenishDecisionRepository.buildLock("style:decide:" + param.getStyleId());
        try {
            if (!lock.tryLock()) {
                // 拿不到锁说明另一路正在决策，直接让调用方稍后重试，不要排队等待
                return ResultDO.buildFailResult("LOCK_FAIL", "该款正在决策中，请稍后重试");
            }

            // 第一层幂等：已有进行中的单就直接返回它
            ResultDO<ReplenishDecisionAggregate> activeResult =
                    replenishDecisionRepository.query(DecisionQuery.activeByStyle(param.getStyleId()));
            if (!activeResult.isSuccess()) {
                return ResultDO.buildFailResult(activeResult.getCode(), activeResult.getMsg());
            }
            ReplenishDecisionAggregate active = activeResult.getData();
            if (active != null) {
                log.info("命中幂等，返回已存在决策单, styleId={}, decisionId={}",
                        param.getStyleId(), active.getId());
                return ResultDO.buildSuccessResult(toResult(active, true));
            }

            // 轮次必须在锁内取，否则两路并发会拿到同一个 round
            ResultDO<Integer> roundResult = replenishDecisionRepository.nextRound(param.getStyleId());
            if (!roundResult.isSuccess()) {
                return ResultDO.buildFailResult(roundResult.getCode(), roundResult.getMsg());
            }
            param.setRound(roundResult.getData());

            ReplenishDecisionAggregate decision = ReplenishDecisionAggregate.open(param);

            ResultDO<Void> saveResult = replenishDecisionRepository.save(decision);
            if (!saveResult.isSuccess()) {
                // 唯一键冲突：说明并发的另一路已经写成功了，按幂等处理而不是报错
                if ("DECISION_DUPLICATED".equals(saveResult.getCode())) {
                    ResultDO<ReplenishDecisionAggregate> retry =
                            replenishDecisionRepository.query(DecisionQuery.activeByStyle(param.getStyleId()));
                    if (retry.isSuccess() && retry.getData() != null) {
                        return ResultDO.buildSuccessResult(toResult(retry.getData(), true));
                    }
                }
                return ResultDO.buildFailResult(saveResult.getCode(), saveResult.getMsg());
            }

            log.info("生成返单决策单, styleId={}, round={}, qty={}, rule={}",
                    param.getStyleId(), decision.getRound(), decision.getTotalQty(), param.getRuleCode());
            return ResultDO.buildSuccessResult(toResult(decision, false));

        } catch (AggregateException e) {
            log.warn("开决策单被拒绝, styleId={}, msg={}", param.getStyleId(), e.getMessage());
            return ResultDO.buildFailResult(e.getCode(), e.getMessage());
        } catch (BizException e) {
            log.warn("开决策单业务异常, styleId={}, msg={}", param.getStyleId(), e.getMessage());
            return ResultDO.buildFailResult(e.getCode(), e.getMessage());
        } catch (Throwable e) {
            log.error("开决策单系统异常, styleId={}", param.getStyleId(), e);
            return ResultDO.buildFailResult("SYSTEM_ERROR", "系统异常");
        } finally {
            lock.unlock();
        }
    }

    @Override
    public ResultDO<ReplenishDecisionResult> approveDecision(ApproveDecisionParam param) {
        if (param == null || param.getDecisionId() == null) {
            return ResultDO.buildFailResult("PARAM_ERROR", "decisionId 不能为空");
        }
        LevelLock lock = replenishDecisionRepository.buildLock("decision:" + param.getDecisionId());
        try {
            if (!lock.tryLock()) {
                return ResultDO.buildFailResult("LOCK_FAIL", "该决策单正在被处理，请稍后重试");
            }
            ResultDO<ReplenishDecisionAggregate> queryResult =
                    replenishDecisionRepository.query(DecisionQuery.byId(param.getDecisionId()));
            if (!queryResult.isSuccess()) {
                return ResultDO.buildFailResult(queryResult.getCode(), queryResult.getMsg());
            }
            ReplenishDecisionAggregate decision = queryResult.getData();
            if (decision == null) {
                return ResultDO.buildFailResult("DECISION_NOT_FOUND", "决策单不存在");
            }

            decision.approve(param);

            ResultDO<Void> saveResult = replenishDecisionRepository.save(decision);
            if (!saveResult.isSuccess()) {
                return ResultDO.buildFailResult(saveResult.getCode(), saveResult.getMsg());
            }
            return ResultDO.buildSuccessResult(toResult(decision, false));

        } catch (AggregateException e) {
            log.warn("审批被拒绝, decisionId={}, msg={}", param.getDecisionId(), e.getMessage());
            return ResultDO.buildFailResult(e.getCode(), e.getMessage());
        } catch (Throwable e) {
            log.error("审批系统异常, decisionId={}", param.getDecisionId(), e);
            return ResultDO.buildFailResult("SYSTEM_ERROR", "系统异常");
        } finally {
            lock.unlock();
        }
    }

    @Override
    public ResultDO<Void> markPendingCapacity(Long decisionId) {
        return transit(decisionId, ReplenishDecisionAggregate::markPendingCapacity);
    }

    @Override
    public ResultDO<Void> markDispatched(Long decisionId) {
        return transit(decisionId, ReplenishDecisionAggregate::markDispatched);
    }

    /** 统一的"加载→流转→保存"模板，避免每个状态都复制一遍 */
    private ResultDO<Void> transit(Long decisionId, java.util.function.Consumer<ReplenishDecisionAggregate> action) {
        LevelLock lock = replenishDecisionRepository.buildLock("decision:" + decisionId);
        try {
            if (!lock.tryLock()) {
                return ResultDO.buildFailResult("LOCK_FAIL", "该决策单正在被处理");
            }
            ResultDO<ReplenishDecisionAggregate> queryResult =
                    replenishDecisionRepository.query(DecisionQuery.byId(decisionId));
            if (!queryResult.isSuccess()) {
                return ResultDO.buildFailResult(queryResult.getCode(), queryResult.getMsg());
            }
            ReplenishDecisionAggregate decision = queryResult.getData();
            if (decision == null) {
                return ResultDO.buildFailResult("DECISION_NOT_FOUND", "决策单不存在");
            }
            action.accept(decision);
            return replenishDecisionRepository.save(decision);
        } catch (AggregateException e) {
            log.warn("状态流转被拒绝, decisionId={}, msg={}", decisionId, e.getMessage());
            return ResultDO.buildFailResult(e.getCode(), e.getMessage());
        } catch (Throwable e) {
            log.error("状态流转异常, decisionId={}", decisionId, e);
            return ResultDO.buildFailResult("SYSTEM_ERROR", "系统异常");
        } finally {
            lock.unlock();
        }
    }

    @Override
    public ResultDO<ReplenishDecisionResult> queryDecision(Long decisionId) {
        ResultDO<ReplenishDecisionAggregate> queryResult =
                replenishDecisionRepository.query(DecisionQuery.byId(decisionId));
        if (!queryResult.isSuccess()) {
            return ResultDO.buildFailResult(queryResult.getCode(), queryResult.getMsg());
        }
        if (queryResult.getData() == null) {
            return ResultDO.buildFailResult("DECISION_NOT_FOUND", "决策单不存在");
        }
        return ResultDO.buildSuccessResult(toResult(queryResult.getData(), false));
    }

    /** 聚合根 → 快照。转换动作放在领域服务里，避免上层直接接触聚合根 */
    private ReplenishDecisionResult toResult(ReplenishDecisionAggregate decision, boolean duplicated) {
        ReplenishDecisionResult result = new ReplenishDecisionResult();
        result.setDecisionId(decision.getId());
        result.setStyleId(decision.getStyleId());
        result.setRound(decision.getRound());
        result.setStatus(decision.getStatus() == null ? null : decision.getStatus().name());
        result.setTotalQty(decision.getTotalQty());
        result.setShadow(decision.isShadow());
        result.setDuplicated(duplicated);

        List<ReplenishDecisionResult.ItemView> items = new java.util.ArrayList<>();
        for (ReplenishDecisionItemEntity item : decision.getItems()) {
            items.add(new ReplenishDecisionResult.ItemView(
                    item.getSkuId(), item.getColorCode(), item.getSizeCode(), item.qtyValue()));
        }
        result.setItems(items);
        return result;
    }
}