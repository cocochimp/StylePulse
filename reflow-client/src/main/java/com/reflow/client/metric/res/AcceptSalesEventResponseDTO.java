package com.reflow.client.metric.res;

/**
 * 销量事件接入结果。
 *
 * <p>返回 {@code accepted=false} 不是错误——重复投递和迟到事件都是正常现象，
 * 调用方（MQ 消费者）拿到它应当照常 ACK，而不是重试。
 * 这个语义差异是幂等设计的关键：把"正常的丢弃"和"需要重试的失败"分开。</p>
 */
public class AcceptSalesEventResponseDTO {

    /** 是否计入了水位 */
    private boolean accepted;

    /** 未计入的原因：DUPLICATED / LATE_EVENT / ... */
    private String reason;

    /** 该款近 24 小时累计销量，便于调用方观测 */
    private Integer windowSales;

    public AcceptSalesEventResponseDTO() {
    }

    public AcceptSalesEventResponseDTO(boolean accepted, String reason, Integer windowSales) {
        this.accepted = accepted;
        this.reason = reason;
        this.windowSales = windowSales;
    }

    public boolean isAccepted() {
        return accepted;
    }

    public void setAccepted(boolean accepted) {
        this.accepted = accepted;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Integer getWindowSales() {
        return windowSales;
    }

    public void setWindowSales(Integer windowSales) {
        this.windowSales = windowSales;
    }
}