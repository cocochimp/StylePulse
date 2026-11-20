package com.reflow.domain.insight.model.param;

import com.reflow.common.base.BaseParam;

/**
 * 影子对比入参：同一款、同一时点，新旧两版规则各算出一个追单量。
 */
public class ShadowCompareParam extends BaseParam {

    private static final long serialVersionUID = 1L;

    private Long styleId;

    /** 线上规则算出的量 */
    private Integer liveQty;
    private Integer liveRuleVersion;

    /** 影子规则算出的量 */
    private Integer shadowQty;
    private Integer shadowRuleVersion;

    public Long getStyleId() {
        return styleId;
    }

    public void setStyleId(Long styleId) {
        this.styleId = styleId;
    }

    public Integer getLiveQty() {
        return liveQty;
    }

    public void setLiveQty(Integer liveQty) {
        this.liveQty = liveQty;
    }

    public Integer getLiveRuleVersion() {
        return liveRuleVersion;
    }

    public void setLiveRuleVersion(Integer liveRuleVersion) {
        this.liveRuleVersion = liveRuleVersion;
    }

    public Integer getShadowQty() {
        return shadowQty;
    }

    public void setShadowQty(Integer shadowQty) {
        this.shadowQty = shadowQty;
    }

    public Integer getShadowRuleVersion() {
        return shadowRuleVersion;
    }

    public void setShadowRuleVersion(Integer shadowRuleVersion) {
        this.shadowRuleVersion = shadowRuleVersion;
    }
}