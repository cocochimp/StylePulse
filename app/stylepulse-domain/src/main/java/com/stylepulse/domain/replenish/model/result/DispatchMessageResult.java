package com.stylepulse.domain.replenish.model.result;

import com.stylepulse.common.base.BaseResult;

/**
 * 待下发的生产指令消息。
 *
 * <p>它是"本地消息表"的领域表达：决策单落库的同事务里写一条待发消息，
 * 再由后台任务投递给工厂。这样"落库成功但下发失败"就不会丢单——
 * 消息还在表里，重试即可。</p>
 */
public class DispatchMessageResult extends BaseResult {

    private static final long serialVersionUID = 1L;

    private final String msgId;
    private final Long decisionId;
    private final String payload;
    private final int retryCount;

    public DispatchMessageResult(String msgId, Long decisionId, String payload, int retryCount) {
        this.msgId = msgId;
        this.decisionId = decisionId;
        this.payload = payload;
        this.retryCount = retryCount;
    }

    public String getMsgId() {
        return msgId;
    }

    public Long getDecisionId() {
        return decisionId;
    }

    public String getPayload() {
        return payload;
    }

    public int getRetryCount() {
        return retryCount;
    }
}