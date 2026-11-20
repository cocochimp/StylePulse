package com.reflow.domain.metric.model.result;

import com.reflow.common.base.BaseResult;

/**
 * 款健康度计算结果（充血：自带判断方法）。
 */
public class StyleHealthResult extends BaseResult {

    private static final long serialVersionUID = 1L;

    /** 动销率 = 窗口销量 / 首单量 */
    private final double sellThroughRate;

    /** 可售天数 = 当前库存 / 日均销量 */
    private final double dos;

    /** 断码率 */
    private final double brokenSizeRate;

    /** 综合健康度 0~100 */
    private final int healthScore;

    public StyleHealthResult(double sellThroughRate, double dos, double brokenSizeRate, int healthScore) {
        this.sellThroughRate = sellThroughRate;
        this.dos = dos;
        this.brokenSizeRate = brokenSizeRate;
        this.healthScore = healthScore;
    }

    public double getSellThroughRate() {
        return sellThroughRate;
    }

    public double getDos() {
        return dos;
    }

    public double getBrokenSizeRate() {
        return brokenSizeRate;
    }

    public int getHealthScore() {
        return healthScore;
    }

    /** 充血方法：判断是否需要关注。阈值属于业务口径，放在 Result 上比让调用方各写一遍更一致 */
    public boolean needsAttention() {
        return this.dos < 5 || this.brokenSizeRate > 0.15;
    }
}