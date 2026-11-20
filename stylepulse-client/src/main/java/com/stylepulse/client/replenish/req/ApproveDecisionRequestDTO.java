package com.stylepulse.client.replenish.req;

import com.stylepulse.common.base.BaseParam;
import com.stylepulse.common.result.ResultDO;

/**
 * 审批决策请求。
 */
public class ApproveDecisionRequestDTO extends BaseParam {

    private static final long serialVersionUID = 1L;

    private Long decisionId;

    /** true 批准，false 驳回 */
    private Boolean approved;

    private String remark;

    public ResultDO<Void> check() {
        if (decisionId == null || decisionId <= 0) {
            return ResultDO.buildFailResult("PARAM_ERROR", "decisionId 非法");
        }
        if (approved == null) {
            return ResultDO.buildFailResult("PARAM_ERROR", "approved 不能为空");
        }
        return ResultDO.buildSuccessResult();
    }

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