package com.reflow.domain.replenish.model.entity;

import com.reflow.common.base.BaseEntity;
import com.reflow.common.field.Field;

/**
 * 决策单的尺码行（实体）。
 *
 * <p>为什么尺码行要作为实体放在决策单聚合内，而不是单独一张表随便查：
 * "总量 = 各行之和"是必须同时成立的不变量，拆到两个聚合就没法保证。
 * 一旦不守恒，工厂收到的生产单和审批看到的数量对不上，是最难查的一类问题。</p>
 */
public class ReplenishDecisionItemEntity extends BaseEntity<Long> {

    private static final long serialVersionUID = 1L;

    private Long skuId;

    private String colorCode;

    private String sizeCode;

    /** 该尺码追单量，用 Field 包装以便区分"未加载"与"0" */
    private Field<Integer> qty;

    public ReplenishDecisionItemEntity() {
    }

    public ReplenishDecisionItemEntity(Long skuId, String colorCode, String sizeCode, Integer qty) {
        this.skuId = skuId;
        this.colorCode = colorCode;
        this.sizeCode = sizeCode;
        this.qty = Field.of(qty);
    }

    public int qtyValue() {
        return qty == null ? 0 : qty.orElse(0);
    }

    public Long getSkuId() {
        return skuId;
    }

    public void setSkuId(Long skuId) {
        this.skuId = skuId;
    }

    public String getColorCode() {
        return colorCode;
    }

    public void setColorCode(String colorCode) {
        this.colorCode = colorCode;
    }

    public String getSizeCode() {
        return sizeCode;
    }

    public void setSizeCode(String sizeCode) {
        this.sizeCode = sizeCode;
    }

    public Field<Integer> getQty() {
        return qty;
    }

    public void setQty(Field<Integer> qty) {
        this.qty = qty;
    }
}