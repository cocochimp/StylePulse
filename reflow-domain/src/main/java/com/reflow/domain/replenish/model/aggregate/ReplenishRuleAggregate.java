package com.reflow.domain.replenish.model.aggregate;

import com.reflow.common.base.BaseAggregate;
import com.reflow.domain.replenish.model.entity.ReplenishRuleEntity;
import com.reflow.domain.replenish.model.param.CalculateQtyParam;
import com.reflow.domain.replenish.model.param.MatchStyleParam;
import com.reflow.domain.replenish.model.result.CalculateQtyResult;

/**
 * 返单规则聚合根（规则+计算模式）。
 *
 * <p>规则+计算模式的形态：<b>聚合根负责"哪条规则生效"，实体负责"这条规则怎么算"</b>。
 * 和写模式的区别只有一点——这里的方法不能改状态，算完就把结果抛出去。</p>
 *
 * <p>为什么还要聚合根而不是直接用实体：规则集合需要"按优先级排序后取第一个命中"，
 * 这个"选哪条"的决策属于聚合层职责；而"怎么算"属于实体。
 * 两者混在一起，实体就会开始关心别的规则，边界就没了。</p>
 */
public class ReplenishRuleAggregate extends BaseAggregate<Long> {

    private static final long serialVersionUID = 1L;

    private String ruleCode;

    /** 优先级，数值越大越优先。高优先级规则用于"特殊品类特殊处理" */
    private Integer priority;

    /** 规则版本：每次改条件/参数都要 +1，决策单会记下它 */
    private Integer ruleVersion;

    private Boolean enabled;

    private ReplenishRuleEntity ruleEntity;

    public ReplenishRuleAggregate() {
    }

    public ReplenishRuleAggregate(Long ruleId, String ruleCode, Integer priority, Integer ruleVersion,
                                  Boolean enabled, ReplenishRuleEntity ruleEntity) {
        setId(ruleId);
        this.ruleCode = ruleCode;
        this.priority = priority;
        this.ruleVersion = ruleVersion;
        this.enabled = enabled;
        this.ruleEntity = ruleEntity;
    }

    /** 规则是否可用：停用的规则即使被索引命中也不能生效 */
    public boolean isAvailable() {
        return Boolean.TRUE.equals(enabled) && ruleEntity != null;
    }

    /** 匹配款（无副作用） */
    public boolean matchStyle(MatchStyleParam param) {
        return isAvailable() && ruleEntity.matchStyle(param);
    }

    /**
     * 计算追单量（无副作用）。
     *
     * <p>调用前的隐含前提：调用方已确认本规则命中了该款。
     * 这里不在内部重复做 matchStyle——重复判断会让"为什么命中了"变得难以追溯，
     * 也让每次都多跑一遍条件比较。</p>
     */
    public CalculateQtyResult calculateQty(CalculateQtyParam param) {
        CalculateQtyResult result = ruleEntity.calculateQty(param);
        // 把"规则身份"补进结果：实体不懂自己属于哪条规则，这是聚合根才知道的事
        result.setRuleId(getId());
        result.setRuleCode(this.ruleCode);
        result.setRuleVersion(this.ruleVersion);
        return result;
    }

    public int priorityValue() {
        return priority == null ? 0 : priority;
    }

    public String getRuleCode() {
        return ruleCode;
    }

    public void setRuleCode(String ruleCode) {
        this.ruleCode = ruleCode;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public Integer getRuleVersion() {
        return ruleVersion;
    }

    public void setRuleVersion(Integer ruleVersion) {
        this.ruleVersion = ruleVersion;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public ReplenishRuleEntity getRuleEntity() {
        return ruleEntity;
    }

    public void setRuleEntity(ReplenishRuleEntity ruleEntity) {
        this.ruleEntity = ruleEntity;
    }
}