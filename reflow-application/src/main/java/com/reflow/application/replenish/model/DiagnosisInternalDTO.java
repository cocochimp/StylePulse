package com.reflow.application.replenish.model;

import com.reflow.common.base.BaseResult;

/**
 * 诊断中间结果（应用层内部流转，不对外暴露）。
 *
 * <p>为什么放 application/model：它只在"诊断服务"和"开单服务"之间传递，
 * 外部系统不需要也不应该依赖它。放在 client 会把内部实现细节变成对外契约，
 * 以后想改字段就得考虑兼容性。</p>
 */
public class DiagnosisInternalDTO extends BaseResult {

    private static final long serialVersionUID = 1L;

    private String stage;
    private double sellThroughRate;
    private double dos;
    private double brokenSizeRate;
    private int healthScore;

    private boolean matched;
    private Long ruleId;
    private String ruleCode;
    private Integer ruleVersion;
    private int qty;
    private boolean belowMoq;
    private int moq;
    private String formulaTrace;
    private String reason;

    public String getStage() {
        return stage;
    }

    public void setStage(String stage) {
        this.stage = stage;
    }

    public double getSellThroughRate() {
        return sellThroughRate;
    }

    public void setSellThroughRate(double sellThroughRate) {
        this.sellThroughRate = sellThroughRate;
    }

    public double getDos() {
        return dos;
    }

    public void setDos(double dos) {
        this.dos = dos;
    }

    public double getBrokenSizeRate() {
        return brokenSizeRate;
    }

    public void setBrokenSizeRate(double brokenSizeRate) {
        this.brokenSizeRate = brokenSizeRate;
    }

    public int getHealthScore() {
        return healthScore;
    }

    public void setHealthScore(int healthScore) {
        this.healthScore = healthScore;
    }

    public boolean isMatched() {
        return matched;
    }

    public void setMatched(boolean matched) {
        this.matched = matched;
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

    public int getQty() {
        return qty;
    }

    public void setQty(int qty) {
        this.qty = qty;
    }

    public boolean isBelowMoq() {
        return belowMoq;
    }

    public void setBelowMoq(boolean belowMoq) {
        this.belowMoq = belowMoq;
    }

    public int getMoq() {
        return moq;
    }

    public void setMoq(int moq) {
        this.moq = moq;
    }

    public String getFormulaTrace() {
        return formulaTrace;
    }

    public void setFormulaTrace(String formulaTrace) {
        this.formulaTrace = formulaTrace;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}