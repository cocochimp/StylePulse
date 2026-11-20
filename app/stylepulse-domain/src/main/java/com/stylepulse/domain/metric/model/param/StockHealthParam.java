package com.stylepulse.domain.metric.model.param;

import com.stylepulse.common.base.BaseParam;

/**
 * 库存健康度计算参数（纯计算模式入参）。
 *
 * <p>纯计算模式的典型特征：<b>所有输入都是值</b>，不持有仓储、不需要加载聚合根。
 * 这么做的好处是它可以被单元测试彻底覆盖，也可以被离线批算复用——
 * 同一套公式既算实时诊断，也算历史回放，结论才可比。</p>
 */
public class StockHealthParam extends BaseParam {

    private static final long serialVersionUID = 1L;

    /** 首单量，动销率的分母 */
    private Integer firstOrderQty;

    /** 窗口内销量 */
    private Integer windowSales;

    /** 窗口天数，用于把销量折算成日均 */
    private Integer windowDays;

    /** 当前总可售库存 */
    private Integer totalAvailableStock;

    /** 断码率（由款聚合根提供） */
    private Double brokenSizeRate;

    public Integer getFirstOrderQty() {
        return firstOrderQty;
    }

    public void setFirstOrderQty(Integer firstOrderQty) {
        this.firstOrderQty = firstOrderQty;
    }

    public Integer getWindowSales() {
        return windowSales;
    }

    public void setWindowSales(Integer windowSales) {
        this.windowSales = windowSales;
    }

    public Integer getWindowDays() {
        return windowDays;
    }

    public void setWindowDays(Integer windowDays) {
        this.windowDays = windowDays;
    }

    public Integer getTotalAvailableStock() {
        return totalAvailableStock;
    }

    public void setTotalAvailableStock(Integer totalAvailableStock) {
        this.totalAvailableStock = totalAvailableStock;
    }

    public Double getBrokenSizeRate() {
        return brokenSizeRate;
    }

    public void setBrokenSizeRate(Double brokenSizeRate) {
        this.brokenSizeRate = brokenSizeRate;
    }
}