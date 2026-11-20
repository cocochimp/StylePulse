package com.reflow.adaptor.capacity.output;

import com.reflow.application.replenish.port.FactoryOrderCommand;
import com.reflow.application.replenish.port.FactoryOrderPort;
import com.reflow.common.result.ResultDO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 工厂下单出口实现（Output Adaptor / 防腐层）。
 *
 * <p>本工程用日志模拟工厂接口，重点在示范三件事：
 * <ol>
 *   <li><b>幂等键显式传递</b>：把 decisionId 作为对方系统的幂等键传过去，
 *       这样重试不会造成重复生产。这一点必须在注释里约定清楚，因为它是跨团队契约。</li>
 *   <li><b>超时与失败要能被上层感知</b>：返回 ResultDO 而不是抛异常，
 *       让应用层决定"释放产能还是重试"。</li>
 *   <li><b>技术适配可以在这里用策略模式</b>：将来接第二家工厂时，
 *       按 factoryId 路由不同客户端，这段代码可以放心用 if/else 或策略——它是技术分支，不是业务分支。</li>
 * </ol>
 */
@Component
public class FactoryOrderAdaptorImpl implements FactoryOrderPort {

    private static final Logger log = LoggerFactory.getLogger(FactoryOrderAdaptorImpl.class);

    /** 模拟开关：置为 false 可演练"下发失败 → 本地消息重试"的完整链路 */
    @Value("${reflow.factory.mock-success:true}")
    private boolean mockSuccess;

    @Override
    public ResultDO<Void> dispatch(FactoryOrderCommand command) {
        if (command == null || command.getDecisionId() == null) {
            return ResultDO.buildFailResult("PARAM_ERROR", "指令缺少幂等键 decisionId");
        }
        if (!mockSuccess) {
            log.warn("模拟工厂接口不可用, decisionId={}", command.getDecisionId());
            return ResultDO.buildFailResult("FACTORY_UNAVAILABLE", "工厂接口暂时不可用");
        }
        // 真实实现：HTTP 调用工厂 MES，header 带 Idempotency-Key = decisionId
        log.info("生产指令已下发工厂: {}", command.toPayload());
        return ResultDO.buildSuccessResult();
    }
}