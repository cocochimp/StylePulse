package com.stylepulse.infrastructure.replenish.mysql.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;

/**
 * 返单规则 PO。
 *
 * <p>条件用 min/max 两列展开，而不是存一个 JSON：规则需要被"人"直接改和看，
 * 展开成列后运营在数据库里也能看明白，加索引也方便。
 * 代价是维度多时列会变多——这是可接受的，因为维度的增长是"业务变更"，不是"数据增长"。</p>
 */
@TableName("t_replenish_rule")
public class ReplenishRulePO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String ruleCode;

    private Integer priority;

    private Integer ruleVersion;

    private Integer enabled;

    /** 类目 CSV，如 "1,2"；空表示不限 */
    private String categoryIds;

    /** 价格带 code CSV，如 "1,2"；空表示不限 */
    private String priceBands;

    private Integer ageDayMin;
    private Integer ageDayMax;

    private Integer dosMin;
    private Integer dosMax;

    /** 动销率区间，单位万分比：8300 = 83% */
    private Integer sellThroughMin;
    private Integer sellThroughMax;

    private BigDecimal cycleFactor;

    private BigDecimal growthFactor;

    private Integer moq;

    private BigDecimal maxMultiplier;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Integer getEnabled() {
        return enabled;
    }

    public void setEnabled(Integer enabled) {
        this.enabled = enabled;
    }

    public String getCategoryIds() {
        return categoryIds;
    }

    public void setCategoryIds(String categoryIds) {
        this.categoryIds = categoryIds;
    }

    public String getPriceBands() {
        return priceBands;
    }

    public void setPriceBands(String priceBands) {
        this.priceBands = priceBands;
    }

    public Integer getAgeDayMin() {
        return ageDayMin;
    }

    public void setAgeDayMin(Integer ageDayMin) {
        this.ageDayMin = ageDayMin;
    }

    public Integer getAgeDayMax() {
        return ageDayMax;
    }

    public void setAgeDayMax(Integer ageDayMax) {
        this.ageDayMax = ageDayMax;
    }

    public Integer getDosMin() {
        return dosMin;
    }

    public void setDosMin(Integer dosMin) {
        this.dosMin = dosMin;
    }

    public Integer getDosMax() {
        return dosMax;
    }

    public void setDosMax(Integer dosMax) {
        this.dosMax = dosMax;
    }

    public Integer getSellThroughMin() {
        return sellThroughMin;
    }

    public void setSellThroughMin(Integer sellThroughMin) {
        this.sellThroughMin = sellThroughMin;
    }

    public Integer getSellThroughMax() {
        return sellThroughMax;
    }

    public void setSellThroughMax(Integer sellThroughMax) {
        this.sellThroughMax = sellThroughMax;
    }

    public BigDecimal getCycleFactor() {
        return cycleFactor;
    }

    public void setCycleFactor(BigDecimal cycleFactor) {
        this.cycleFactor = cycleFactor;
    }

    public BigDecimal getGrowthFactor() {
        return growthFactor;
    }

    public void setGrowthFactor(BigDecimal growthFactor) {
        this.growthFactor = growthFactor;
    }

    public Integer getMoq() {
        return moq;
    }

    public void setMoq(Integer moq) {
        this.moq = moq;
    }

    public BigDecimal getMaxMultiplier() {
        return maxMultiplier;
    }

    public void setMaxMultiplier(BigDecimal maxMultiplier) {
        this.maxMultiplier = maxMultiplier;
    }
}