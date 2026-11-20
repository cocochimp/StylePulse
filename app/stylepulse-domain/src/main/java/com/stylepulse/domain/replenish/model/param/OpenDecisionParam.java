package com.stylepulse.domain.replenish.model.param;

import com.stylepulse.common.base.BaseParam;

import java.util.ArrayList;
import java.util.List;

/**
 * 开启返单决策入参。
 *
 * <p>追单量与尺码拆分结果在这里传入，而不是让领域服务自己去算：
 * 因为"算多少"依赖跨上下文的指标（销量窗口），属于应用层编排职责。
 * 领域服务拿到的是<b>已经算好的事实</b>，它只负责"这个事实能不能落成一张合法的单"。</p>
 */
public class OpenDecisionParam extends BaseParam {

    private static final long serialVersionUID = 1L;

    private Long styleId;

    /** 第几次返单，由仓储在锁内取 max(round)+1 得到 */
    private Integer round;

    private Long ruleId;
    private String ruleCode;
    private Integer ruleVersion;

    private Integer totalQty;

    /** 触发来源：SCHEDULE / EVENT / MANUAL */
    private String triggerSource;

    /** 是否为影子决策（只算不下发） */
    private Boolean shadow;

    private List<DecisionItemParam> items = new ArrayList<>();

    public Long getStyleId() {
        return styleId;
    }

    public void setStyleId(Long styleId) {
        this.styleId = styleId;
    }

    public Integer getRound() {
        return round;
    }

    public void setRound(Integer round) {
        this.round = round;
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

    public Integer getTotalQty() {
        return totalQty;
    }

    public void setTotalQty(Integer totalQty) {
        this.totalQty = totalQty;
    }

    public String getTriggerSource() {
        return triggerSource;
    }

    public void setTriggerSource(String triggerSource) {
        this.triggerSource = triggerSource;
    }

    public Boolean getShadow() {
        return shadow;
    }

    public void setShadow(Boolean shadow) {
        this.shadow = shadow;
    }

    public List<DecisionItemParam> getItems() {
        return items;
    }

    public void setItems(List<DecisionItemParam> items) {
        this.items = items;
    }
}