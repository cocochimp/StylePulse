package com.reflow.client.replenish.res;

/**
 * 款诊断结果。
 *
 * <p>这个 DTO 是本系统"算得准"的对外证明：把动销率、DOS、断码率、健康度评分、
 * 命中的规则和规则版本一起返回。运营看到的是一个可解释的结论，
 * 而不是一个黑盒数字——这正是返单决策能被追责、被复盘的前提。</p>
 */
public class DiagnoseStyleResponseDTO {

    private Long styleId;

    /** 当前生命周期阶段 */
    private String stage;

    /** 动销率：窗口销量 / 首单量，保留 4 位小数 */
    private Double sellThroughRate;

    /** 可售天数 DOS：当前可售库存 / 日均销量 */
    private Double dos;

    /** 断码率：有销量但库存为 0 的 SKU 占比 */
    private Double brokenSizeRate;

    /** 健康度评分 0~100 */
    private Integer healthScore;

    /** 命中的规则编码；未命中为 null */
    private String matchedRuleCode;

    /** 命中规则的版本号，复盘时用它定位"当时按哪版口径算的" */
    private Integer ruleVersion;

    /** 建议追单量（未拆尺码）；0 表示不追 */
    private Integer suggestedQty;

    /** 是否因为低于 MOQ 而不追 */
    private Boolean belowMoq;

    /** 未命中规则的原因 */
    private String reason;

    public Long getStyleId() {
        return styleId;
    }

    public void setStyleId(Long styleId) {
        this.styleId = styleId;
    }

    public String getStage() {
        return stage;
    }

    public void setStage(String stage) {
        this.stage = stage;
    }

    public Double getSellThroughRate() {
        return sellThroughRate;
    }

    public void setSellThroughRate(Double sellThroughRate) {
        this.sellThroughRate = sellThroughRate;
    }

    public Double getDos() {
        return dos;
    }

    public void setDos(Double dos) {
        this.dos = dos;
    }

    public Double getBrokenSizeRate() {
        return brokenSizeRate;
    }

    public void setBrokenSizeRate(Double brokenSizeRate) {
        this.brokenSizeRate = brokenSizeRate;
    }

    public Integer getHealthScore() {
        return healthScore;
    }

    public void setHealthScore(Integer healthScore) {
        this.healthScore = healthScore;
    }

    public String getMatchedRuleCode() {
        return matchedRuleCode;
    }

    public void setMatchedRuleCode(String matchedRuleCode) {
        this.matchedRuleCode = matchedRuleCode;
    }

    public Integer getRuleVersion() {
        return ruleVersion;
    }

    public void setRuleVersion(Integer ruleVersion) {
        this.ruleVersion = ruleVersion;
    }

    public Integer getSuggestedQty() {
        return suggestedQty;
    }

    public void setSuggestedQty(Integer suggestedQty) {
        this.suggestedQty = suggestedQty;
    }

    public Boolean getBelowMoq() {
        return belowMoq;
    }

    public void setBelowMoq(Boolean belowMoq) {
        this.belowMoq = belowMoq;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}