package com.reflow.domain.insight.model.result;

import com.reflow.common.base.BaseResult;

/**
 * 影子对比结果。
 *
 * <p>影子模式（双跑）是"改规则敢不敢上线"的答案：
 * 新规则只算不下发，跑一到两周后看差异，再决定切不切。
 * 这个结果对象就是差异本身。</p>
 */
public class ShadowCompareResult extends BaseResult {

    private static final long serialVersionUID = 1L;

    private final int diffQty;
    private final double diffRate;

    /** 判定建议：SAME / SHADOW_HIGHER / SHADOW_LOWER */
    private final String verdict;

    public ShadowCompareResult(int diffQty, double diffRate, String verdict) {
        this.diffQty = diffQty;
        this.diffRate = diffRate;
        this.verdict = verdict;
    }

    public int getDiffQty() {
        return diffQty;
    }

    public double getDiffRate() {
        return diffRate;
    }

    public String getVerdict() {
        return verdict;
    }

    /** 差异是否大到需要人工复核（10% 以上） */
    public boolean needsManualReview() {
        return Math.abs(diffRate) >= 0.10D;
    }
}