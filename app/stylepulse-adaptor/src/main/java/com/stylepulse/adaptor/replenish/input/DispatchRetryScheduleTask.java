package com.stylepulse.adaptor.replenish.input;

import com.stylepulse.application.replenish.scenario.ReplenishDispatchAppServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 本地消息补投定时任务（Input Adaptor 的一种）。
 *
 * <p>为什么定时任务也算 Adaptor：它和 Controller 本质一样——都是"外部触发进入系统"的入口，
 * 区别只是触发源是时钟而不是 HTTP。放在 adaptor/input 下，
 * 可以保证"系统的入口都在同一个地方"，排查问题时不会漏掉定时任务。</p>
 *
 * <p>这里刻意写得很薄：真正的补偿逻辑在应用服务里，
 * 因为它是"业务上该怎么补"，不是"什么时候补"。</p>
 */
@Component
public class DispatchRetryScheduleTask {

    private static final Logger log = LoggerFactory.getLogger(DispatchRetryScheduleTask.class);

    private final ReplenishDispatchAppServiceImpl dispatchAppService;

    public DispatchRetryScheduleTask(ReplenishDispatchAppServiceImpl dispatchAppService) {
        this.dispatchAppService = dispatchAppService;
    }

    /** 每 30 秒扫一次待补投消息 */
    @Scheduled(fixedDelayString = "${stylepulse.dispatch.retry-interval-millis:30000}")
    public void retryPendingDispatch() {
        try {
            int success = dispatchAppService.retryPending();
            if (success > 0) {
                log.info("本地消息补投任务完成，成功 {} 条", success);
            }
        } catch (Throwable e) {
            // 定时任务绝不能把异常抛出去，否则调度线程会被标记为异常并影响后续调度
            log.error("本地消息补投任务异常", e);
        }
    }
}