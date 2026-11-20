package com.stylepulse.domain.replenish.model.result;

import com.stylepulse.common.base.BaseResult;

/**
 * 追单量计算结果。
 *
 * <p>除了数量，还带回三个"可解释"字段：命中规则、规则版本、计算过程。
 * 这三个字段是复盘的前提——运营问"为什么这款只追 120 件"时，
 * 不需要去翻代码和日志，直接看这条记录。</p>
 */
public class CalculateQtyResult extends BaseResult {

    private static final long serialVersionUID = 1L;

    private int qty;
    private Long ruleId;
    private String ruleCode;
    private Integer ruleVersion;

    /** 是否因低于 MOQ 而不追 */
    private boolean belowMoq;

    /** 本次计算所用的 MOQ，供拆量阶段复用同一口径 */
    private int moq;

    /** 是否被"追单上限倍数"截断 */
    private boolean cappedByMultiplier;

    /** 公式展开过程，如 200×0.83×1.2×1.0=199，cap=600 */
    private String formulaTrace;

    public CalculateQtyResult() {
    }

    public CalculateQtyResult(int qty) {
        this.qty = qty;
    }

    public int getQty() {
        return qty;
    }

    public void setQty(int qty) {
        this.qty = qty;
    }

    public Long getRuleId() {
        return ruleId;
    }

    public void setRuleId(Long ruleId) {
        this.ruleId = ruleId;
    }

    public String getRuleCode() {
        return ruleCode;
    }

    public void setRuleCode(String ruleCode) {
        this.ruleCode = ruleCode;
    }

    public Integer getRuleVersion() {
        return ruleVersion;
    }

    public void setRuleVersion(Integer ruleVersion) {
        this.ruleVersion = ruleVersion;
    }

    public int getMoq() {
        return moq;
    }

    public void setMoq(int moq) {
        this.moq = moq;
    }

    public boolean isBelowMoq() {
        return belowMoq;
    }

    public void setBelowMoq(boolean belowMoq) {
        this.belowMoq = belowMoq;
    }

    public boolean isCappedByMultiplier() {
        return cappedByMultiplier;
    }

    public void setCappedByMultiplier(boolean cappedByMultiplier) {
        this.cappedByMultiplier = cappedByMultiplier;
    }

    public String getFormulaTrace() {
        return formulaTrace;
    }

    public void setFormulaTrace(String formulaTrace) {
        this.formulaTrace = formulaTrace;
    }

    /** 充血方法：是否值得为它开决策单 */
    public boolean shouldReplenish() {
        return qty > 0 && !belowMoq;
    }
}