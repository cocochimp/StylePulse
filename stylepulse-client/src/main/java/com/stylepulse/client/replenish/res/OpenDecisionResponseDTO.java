package com.stylepulse.client.replenish.res;

/**
 * 开启决策结果。
 *
 * <p>{@code duplicated=true} 是本接口最重要的语义：三路并发触发时，
 * 只有一路真正创建，其余返回已存在的决策单。这不是失败，所以 code 仍然是 0，
 * 调用方按"幂等返回"处理即可——这比返回错误码让调用方去重试要友好得多。</p>
 */
public class OpenDecisionResponseDTO {

    private Long decisionId;
    private Long styleId;

    /** 第几次返单（1 开始） */
    private Integer round;

    private Integer totalQty;
    private String status;

    /** true 表示本次请求命中了幂等，返回的是已存在的决策单 */
    private boolean duplicated;

    public Long getDecisionId() {
        return decisionId;
    }

    public void setDecisionId(Long decisionId) {
        this.decisionId = decisionId;
    }

    public Long getStyleId() {
        return styleId;
    }

    public void setStyleId(Long styleId) {
        this.styleId = styleId;
    }

    public Integer getRound() {
        return round;
    }

    public void setRound(Integer round) {
        this.round = round;
    }

    public Integer getTotalQty() {
        return totalQty;
    }

    public void setTotalQty(Integer totalQty) {
        this.totalQty = totalQty;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isDuplicated() {
        return duplicated;
    }

    public void setDuplicated(boolean duplicated) {
        this.duplicated = duplicated;
    }
}