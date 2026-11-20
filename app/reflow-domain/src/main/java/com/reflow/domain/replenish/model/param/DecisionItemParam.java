package com.reflow.domain.replenish.model.param;

import com.reflow.common.base.BaseParam;

/**
 * 决策单尺码行入参（开单时传入）。
 */
public class DecisionItemParam extends BaseParam {

    private static final long serialVersionUID = 1L;

    private Long skuId;
    private String colorCode;
    private String sizeCode;
    private Integer qty;

    public DecisionItemParam() {
    }

    public DecisionItemParam(Long skuId, String colorCode, String sizeCode, Integer qty) {
        this.skuId = skuId;
        this.colorCode = colorCode;
        this.sizeCode = sizeCode;
        this.qty = qty;
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

    public Integer getQty() {
        return qty;
    }

    public void setQty(Integer qty) {
        this.qty = qty;
    }
}