package com.reflow.domain.replenish.model.param;

import com.reflow.common.base.BaseParam;

/**
 * 拆量算法的单个 SKU 输入。
 *
 * <p>刻意不用 {@code SkuEntity}：实体属于 style 聚合，跨上下文传实体会把
 * "谁能改库存"这条边界打穿。这里只传拆量需要的四个值，多余字段一个不要。</p>
 */
public class SkuAllocItemParam extends BaseParam {

    private static final long serialVersionUID = 1L;

    private Long skuId;
    private String colorCode;
    private String sizeCode;
    private Integer availableStock;
    private Integer recentSales;

    public SkuAllocItemParam() {
    }

    public SkuAllocItemParam(Long skuId, String colorCode, String sizeCode, Integer availableStock, Integer recentSales) {
        this.skuId = skuId;
        this.colorCode = colorCode;
        this.sizeCode = sizeCode;
        this.availableStock = availableStock;
        this.recentSales = recentSales;
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

    public Integer getAvailableStock() {
        return availableStock;
    }

    public void setAvailableStock(Integer availableStock) {
        this.availableStock = availableStock;
    }

    public Integer getRecentSales() {
        return recentSales;
    }

    public void setRecentSales(Integer recentSales) {
        this.recentSales = recentSales;
    }
}