package com.stylepulse.client.capacity.res;

/**
 * 产能预占结果。
 *
 * <p>为什么失败也要 200 + 业务码：产能不足是常态（尤其大促），
 * 用 HTTP 500 表达会让上游误判成系统故障进而重试，反而加剧争抢。
 * 这里明确返回 {@code success=false} + {@code remainQty}，调用方可以直接展示"还剩多少"。</p>
 */
public class ReserveCapacityResponseDTO {

    private String holdId;
    private boolean success;
    private Integer remainQty;
    private String reason;

    public String getHoldId() {
        return holdId;
    }

    public void setHoldId(String holdId) {
        this.holdId = holdId;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public Integer getRemainQty() {
        return remainQty;
    }

    public void setRemainQty(Integer remainQty) {
        this.remainQty = remainQty;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}