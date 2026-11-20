package com.stylepulse.adaptor.capacity.input;

import com.stylepulse.domain.capacity.service.CapacityDomainService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 产能超时回收任务。
 *
 * <p>为什么必须有它：预占是"承诺"，但承诺会失败——下发超时、进程崩溃、人工中断。
 * 如果不回收，产能池会被这些"没人认领的承诺"一点点吃光，最后表现为
 * "明明没人占，系统却说产能不足"，这是最难排查的一类产能故障。</p>
 *
 * <p>为什么不用 Redis key TTL 自动回收：TTL 挂在整个槽位 key 上，
 * 没法只让某一笔预占过期。所以过期时间记在 DB 的 hold 行上，由这个任务驱动回收。</p>
 */
@Component
public class CapacityRecycleScheduleTask {

    private static final Logger log = LoggerFactory.getLogger(CapacityRecycleScheduleTask.class);

    private final CapacityDomainService capacityDomainService;

    public CapacityRecycleScheduleTask(CapacityDomainService capacityDomainService) {
        this.capacityDomainService = capacityDomainService;
    }

    /** 每 60 秒扫一次超时预占 */
    @Scheduled(fixedDelayString = "${stylepulse.capacity.recycle-interval-millis:60000}")
    public void recycleExpiredHolds() {
        try {
            capacityDomainService.releaseExpired();
        } catch (Throwable e) {
            log.error("产能回收任务异常", e);
        }
    }
}