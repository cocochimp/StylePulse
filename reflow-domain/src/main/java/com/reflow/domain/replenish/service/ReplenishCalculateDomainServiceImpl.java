package com.reflow.domain.replenish.service;

import com.reflow.common.exception.AggregateException;
import com.reflow.common.exception.BizException;
import com.reflow.common.result.ResultDO;
import com.reflow.domain.replenish.model.aggregate.ReplenishRuleAggregate;
import com.reflow.domain.replenish.model.param.CalculateQtyParam;
import com.reflow.domain.replenish.model.result.CalculateQtyResult;
import com.reflow.domain.replenish.repository.ReplenishRuleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

/**
 * 追单量计算领域服务实现（规则+计算模式的标准骨架）。
 *
 * <p>三步：查规则 → 排序 → 取第一条命中的算。
 * 这里没有任何 if-else 业务判断，因为所有条件判断都在规则实体里——
 * 这也是规则+计算模式最舒服的地方：换规则不用改这个类。</p>
 */
@Service
public class ReplenishCalculateDomainServiceImpl implements ReplenishCalculateDomainService {

    private static final Logger log = LoggerFactory.getLogger(ReplenishCalculateDomainServiceImpl.class);

    private final ReplenishRuleRepository replenishRuleRepository;

    public ReplenishCalculateDomainServiceImpl(ReplenishRuleRepository replenishRuleRepository) {
        this.replenishRuleRepository = replenishRuleRepository;
    }

    @Override
    public ResultDO<CalculateQtyResult> calculateQty(CalculateQtyParam param) {
        try {
            if (param == null || param.getMatchStyleParam() == null) {
                throw new BizException("PARAM_ERROR", "缺少匹配条件");
            }

            // 仓储实现内部用倒排索引 + 位图交集做粗筛，返回的已是候选集
            ResultDO<List<ReplenishRuleAggregate>> ruleResult =
                    replenishRuleRepository.queryMatchedRules(param.getMatchStyleParam());
            if (!ruleResult.isSuccess()) {
                return ResultDO.buildFailResult(ruleResult.getCode(), ruleResult.getMsg());
            }
            List<ReplenishRuleAggregate> rules = ruleResult.getData();
            if (rules == null || rules.isEmpty()) {
                return ResultDO.buildFailResult("RULE_NOT_MATCHED", "没有命中任何返单规则");
            }

            // 优先级降序：特殊品类的高优先规则先赢
            rules.sort(Comparator.comparingInt(ReplenishRuleAggregate::priorityValue).reversed());

            for (ReplenishRuleAggregate rule : rules) {
                if (rule.matchStyle(param.getMatchStyleParam())) {
                    CalculateQtyResult result = rule.calculateQty(param);
                    log.debug("命中规则, styleId={}, ruleCode={}, qty={}",
                            param.getMatchStyleParam().getStyleId(), result.getRuleCode(), result.getQty());
                    return ResultDO.buildSuccessResult(result);
                }
            }
            return ResultDO.buildFailResult("RULE_NOT_MATCHED", "候选规则均未精确命中");

        } catch (AggregateException e) {
            log.warn("追单量计算被拒绝, msg={}", e.getMessage());
            return ResultDO.buildFailResult(e.getCode(), e.getMessage());
        } catch (BizException e) {
            log.warn("追单量计算业务异常, msg={}", e.getMessage());
            return ResultDO.buildFailResult(e.getCode(), e.getMessage());
        } catch (Throwable e) {
            log.error("追单量计算系统异常, param={}", param, e);
            return ResultDO.buildFailResult("SYSTEM_ERROR", "系统异常");
        }
    }
}