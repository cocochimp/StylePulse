package com.stylepulse.domain.replenish.model.param;

import com.stylepulse.common.base.BaseParam;
import com.stylepulse.model.replenish.DecisionStatusEnum;

import java.util.List;

/**
 * 决策单查询条件。
 */
public class DecisionQuery extends BaseParam {

    private static final long serialVersionUID = 1L;

    private Long decisionId;
    private Long styleId;
    private List<DecisionStatusEnum> statuses;
    private Integer limit;

    public static DecisionQuery byId(Long decisionId) {
        DecisionQuery query = new DecisionQuery();
        query.setDecisionId(decisionId);
        return query;
    }

    public static DecisionQuery activeByStyle(Long styleId) {
        DecisionQuery query = new DecisionQuery();
        query.setStyleId(styleId);
        // "进行中"= 尚未终态的状态集合
        query.setStatuses(List.of(
                DecisionStatusEnum.PENDING_REVIEW,
                DecisionStatusEnum.APPROVED,
                DecisionStatusEnum.PENDING_CAPACITY,
                DecisionStatusEnum.DISPATCHED,
                DecisionStatusEnum.IN_PRODUCTION));
        return query;
    }

    public Long getDecisionId() {
        return decisionId;
    }

    public void setDecisionId(Long decisionId) {
        this.decisionId = decisionId;
    }

    public Long getStyleId() {
        return styleId;
    }

    public void setStyleId(Long styleId) {
        this.styleId = styleId;
    }

    public List<DecisionStatusEnum> getStatuses() {
        return statuses;
    }

    public void setStatuses(List<DecisionStatusEnum> statuses) {
        this.statuses = statuses;
    }

    public Integer getLimit() {
        return limit;
    }

    public void setLimit(Integer limit) {
        this.limit = limit;
    }
}