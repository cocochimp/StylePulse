package com.stylepulse.domain.capacity.model.param;

import com.stylepulse.common.base.BaseParam;
import com.stylepulse.model.common.BusinessDate;

/**
 * 产能预占入参。
 *
 * <p>三个维度定位一个产能槽：工厂 + 产线 + 业务日。
 * 少了任何一维都会变成"同一个工厂所有产线共享一个池子"，
 * 排产时就会出现"这条线接了两倍的活"。</p>
 */
public class ReserveCapacityParam extends BaseParam {

    private static final long serialVersionUID = 1L;

    private Long factoryId;
    private Long lineId;
    private BusinessDate bizDate;
    private Integer qty;
    private Long decisionId;

    public Long getFactoryId() {
        return factoryId;
    }

    public void setFactoryId(Long factoryId) {
        this.factoryId = factoryId;
    }

    public Long getLineId() {
        return lineId;
    }

    public void setLineId(Long lineId) {
        this.lineId = lineId;
    }

    public BusinessDate getBizDate() {
        return bizDate;
    }

    public void setBizDate(BusinessDate bizDate) {
        this.bizDate = bizDate;
    }

    public Integer getQty() {
        return qty;
    }

    public void setQty(Integer qty) {
        this.qty = qty;
    }

    public Long getDecisionId() {
        return decisionId;
    }

    public void setDecisionId(Long decisionId) {
        this.decisionId = decisionId;
    }

    /** 槽位唯一标识，格式工厂:产线:日期，直接作为 Redis key 与聚合根 ID */
    public String slotKey() {
        return factoryId + ":" + lineId + ":" + bizDate;
    }
}