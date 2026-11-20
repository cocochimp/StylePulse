package com.reflow.infrastructure.capacity.mysql.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 产能槽账本 PO（工厂 × 产线 × 业务日的总产能）。
 *
 * <p>它只存"总量"，不存"已用量"：已用量是持有记录（hold）的聚合结果。
 * 这是账本设计的基本纪律——<b>事实只记一次</b>。
 * 如果这里再冗余一个 used 字段，就得处理"两个数不一致时信谁"的问题。</p>
 */
@TableName("t_capacity_slot")
public class CapacitySlotPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long factoryId;

    private Long lineId;

    /** yyyy-MM-dd */
    private String bizDate;

    private Integer totalQty;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public String getBizDate() {
        return bizDate;
    }

    public void setBizDate(String bizDate) {
        this.bizDate = bizDate;
    }

    public Integer getTotalQty() {
        return totalQty;
    }

    public void setTotalQty(Integer totalQty) {
        this.totalQty = totalQty;
    }
}