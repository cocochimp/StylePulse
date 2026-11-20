package com.stylepulse.domain.metric.model.result;

import com.stylepulse.common.base.BaseResult;

/**
 * 销量事件处理结果。
 *
 * <p>{@code accepted=false} 有两种原因，且处理方式完全不同：
 * <ul>
 *   <li>DUPLICATED：重复投递，正常现象，直接 ACK</li>
 *   <li>LATE_EVENT：乱序迟到，进补偿队列，不是错误</li>
 * </ul>
 * 把两者区分开写进结果，调用方就不需要自己去猜。</p>
 */
public class AcceptSalesEventResult extends BaseResult {

    private static final long serialVersionUID = 1L;

    public static final String REASON_DUPLICATED = "DUPLICATED";
    public static final String REASON_LATE = "LATE_EVENT";

    private final boolean accepted;
    private final String reason;
    private final Long lastEventTime;

    private AcceptSalesEventResult(boolean accepted, String reason, Long lastEventTime) {
        this.accepted = accepted;
        this.reason = reason;
        this.lastEventTime = lastEventTime;
    }

    public static AcceptSalesEventResult accepted(Long lastEventTime) {
        return new AcceptSalesEventResult(true, null, lastEventTime);
    }

    public static AcceptSalesEventResult duplicated(Long lastEventTime) {
        return new AcceptSalesEventResult(false, REASON_DUPLICATED, lastEventTime);
    }

    public static AcceptSalesEventResult late(Long lastEventTime) {
        return new AcceptSalesEventResult(false, REASON_LATE, lastEventTime);
    }

    public boolean isAccepted() {
        return accepted;
    }

    public String getReason() {
        return reason;
    }

    public Long getLastEventTime() {
        return lastEventTime;
    }
}