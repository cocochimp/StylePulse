package com.reflow.client.replenish.res;

/**
 * 审批结果。
 *
 * <p>{@code capacityHoldId} 为空表示这批货还没占到产能，决策单停在 PENDING_CAPACITY。
 * 把这个状态透出给运营，比悄悄失败要好——运营能立刻知道"卡在产能"而不是"系统坏了"。</p>
 */
public class ApproveDecisionResponseDTO {

    private Long decisionId;
    private String status;
    private Integer totalQty;

    /** 产能预占凭证；为空表示未占到 */
    private String capacityHoldId;

    /** 生产指令是否已下发 */
    private Boolean dispatched;

    public Long getDecisionId() {
        return decisionId;
    }

    public void setDecisionId(Long decisionId) {
        this.decisionId = decisionId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getTotalQty() {
        return totalQty;
    }

    public void setTotalQty(Integer totalQty) {
        this.totalQty = totalQty;
    }

    public String getCapacityHoldId() {
        return capacityHoldId;
    }

    public void setCapacityHoldId(String capacityHoldId) {
        this.capacityHoldId = capacityHoldId;
    }

    public Boolean getDispatched() {
        return dispatched;
    }

    public void setDispatched(Boolean dispatched) {
        this.dispatched = dispatched;
    }
}