package com.reflow.domain.replenish.model.param;

import com.reflow.common.base.BaseParam;

import java.util.ArrayList;
import java.util.List;

/**
 * 尺码拆量入参（纯计算模式）。
 */
public class SizeAllocationParam extends BaseParam {

    private static final long serialVersionUID = 1L;

    private Long styleId;

    /** 款级追单总量 */
    private Integer totalQty;

    /** 参与拆分的 SKU */
    private List<SkuAllocItemParam> skus = new ArrayList<>();

    /** 最小起订量：单 SKU 要么 0，要么 ≥ moq */
    private Integer moq;

    /** 尺码曲线懒加载接口，由应用层注入 */
    private SizeCurveProvider curveProvider;

    public Long getStyleId() {
        return styleId;
    }

    public void setStyleId(Long styleId) {
        this.styleId = styleId;
    }

    public Integer getTotalQty() {
        return totalQty;
    }

    public void setTotalQty(Integer totalQty) {
        this.totalQty = totalQty;
    }

    public List<SkuAllocItemParam> getSkus() {
        return skus;
    }

    public void setSkus(List<SkuAllocItemParam> skus) {
        this.skus = skus;
    }

    public Integer getMoq() {
        return moq;
    }

    public void setMoq(Integer moq) {
        this.moq = moq;
    }

    public SizeCurveProvider getCurveProvider() {
        return curveProvider;
    }

    public void setCurveProvider(SizeCurveProvider curveProvider) {
        this.curveProvider = curveProvider;
    }
}