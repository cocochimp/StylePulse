package com.stylepulse.application.replenish.scenario;

import com.stylepulse.application.replenish.assembler.FactoryOrderCommandAssembler;
import com.stylepulse.application.replenish.port.FactoryOrderCommand;
import com.stylepulse.application.replenish.port.FactoryOrderPort;
import com.stylepulse.common.result.ResultDO;
import com.stylepulse.domain.replenish.model.result.DispatchMessageResult;
import com.stylepulse.domain.replenish.model.result.ReplenishDecisionResult;
import com.stylepulse.domain.replenish.repository.DispatchMessageRepository;
import com.stylepulse.domain.replenish.service.ReplenishDomainService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 生产指令补投服务（本地消息表的消费端）。
 *
 * <p>它回答一个必须回答的问题：<b>如果调用工厂时超时了，但我们并不知道对方到底收没收到，怎么办？</b>
 * 答案是"重试 + 幂等"，而幂等由工厂侧按 decisionId 去重保证（本工程里用日志模拟）。</p>
 *
 * <p>这套机制存在的意义：决策单已经落库、产能已经预占，如果因为一次网络抖动就丢单，
 * 运营第二天才会发现——那时追单窗口已经过了。</p>
 */
@Service
public class ReplenishDispatchAppServiceImpl {

    private static final Logger log = LoggerFactory.getLogger(ReplenishDispatchAppServiceImpl.class);

    /** 单次补投上限，避免一个坏掉的工厂拖垮整个任务 */
    private static final int BATCH_SIZE = 50;

    /** 失败后的重试间隔（秒）。真实项目应做指数退避 */
    private static final int RETRY_DELAY_SECONDS = 60;

    private final DispatchMessageRepository dispatchMessageRepository;
    private final ReplenishDomainService replenishDomainService;
    private final FactoryOrderCommandAssembler commandAssembler;
    private final FactoryOrderPort factoryOrderPort;

    public ReplenishDispatchAppServiceImpl(DispatchMessageRepository dispatchMessageRepository,
                                           ReplenishDomainService replenishDomainService,
                                           FactoryOrderCommandAssembler commandAssembler,
                                           FactoryOrderPort factoryOrderPort) {
        this.dispatchMessageRepository = dispatchMessageRepository;
        this.replenishDomainService = replenishDomainService;
        this.commandAssembler = commandAssembler;
        this.factoryOrderPort = factoryOrderPort;
    }

    /**
     * 补投一批待发消息。
     *
     * @return 本批成功条数，便于监控
     */
    public int retryPending() {
        ResultDO<List<DispatchMessageResult>> pendingResult =
                dispatchMessageRepository.queryPending(BATCH_SIZE);
        if (!pendingResult.isSuccess() || pendingResult.getData() == null || pendingResult.getData().isEmpty()) {
            return 0;
        }

        int success = 0;
        for (DispatchMessageResult message : pendingResult.getData()) {
            try {
                ResultDO<ReplenishDecisionResult> decisionResult =
                        replenishDomainService.queryDecision(message.getDecisionId());
                if (!decisionResult.isSuccess() || decisionResult.getData() == null) {
                    // 决策单没了（被删或数据异常），消息只能作废，否则会永远重试
                    dispatchMessageRepository.markAck(message.getMsgId());
                    log.warn("补偿消息对应的决策单不存在，作废消息, msgId={}", message.getMsgId());
                    continue;
                }

                ReplenishDecisionResult decision = decisionResult.getData();
                if (!"APPROVED".equals(decision.getStatus())) {
                    // 已经是下发/生产/到仓等状态，说明首次其实成功了，只是没收到 ACK
                    dispatchMessageRepository.markAck(message.getMsgId());
                    continue;
                }

                FactoryOrderCommand command = commandAssembler.build(decision);
                ResultDO<Void> dispatchResult = factoryOrderPort.dispatch(command);
                if (dispatchResult.isSuccess()) {
                    dispatchMessageRepository.markSent(message.getMsgId());
                    dispatchMessageRepository.markAck(message.getMsgId());
                    replenishDomainService.markDispatched(decision.getDecisionId());
                    success++;
                } else {
                    dispatchMessageRepository.markFailed(message.getMsgId(), RETRY_DELAY_SECONDS);
                }
            } catch (Throwable e) {
                log.error("补偿投递异常, msgId={}", message.getMsgId(), e);
                dispatchMessageRepository.markFailed(message.getMsgId(), RETRY_DELAY_SECONDS);
            }
        }
        if (success > 0) {
            log.info("本地消息补投完成，成功 {} 条", success);
        }
        return success;
    }
}