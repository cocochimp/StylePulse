package com.stylepulse.infrastructure.replenish.mysql.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 返单决策单 PO。
 *
 * <p>唯一索引 {@code uk_style_round(style_id, round)} 建在表上，不在代码里。
 * 这是 T3 的第三层防护：锁会超时、CAS 会被绕过，但数据库的唯一约束不会失效。</p>
 */
@TableName("t_replenish_decision")
public class ReplenishDecisionPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long styleId;

    private Integer round;

    private Long ruleId;

    private String ruleCode;

    private Integer ruleVersion;

    private Integer totalQty;

    private Integer shadow;

    private String triggerSource;

    private String status;

    private Integer version;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public Integer getShadow() {
        return shadow;
    }

    public void setShadow(Integer shadow) {
        this.shadow = shadow;
    }

    public String getTriggerSource() {
        return triggerSource;
    }

    public void setTriggerSource(String triggerSource) {
        this.triggerSource = triggerSource;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }
}