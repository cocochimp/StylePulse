package com.stylepulse.domain.replenish.model.param;

import com.stylepulse.common.base.BaseParam;

/**
 * 审批决策入参。
 */
public class ApproveDecisionParam extends BaseParam {

    private static final long serialVersionUID = 1L;

    private Long decisionId;

    private Boolean approved;

    private String remark;

    public Long getDecisionId() {
        return decisionId;
    }

    public void setDecisionId(Long decisionId) {
        this.decisionId = decisionId;
    }

    public Boolean getApproved() {
        return approved;
    }

    public void setApproved(Boolean approved) {
        this.approved = approved;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}