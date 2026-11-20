package com.reflow.domain.style.model.param;

import com.reflow.common.base.BaseParam;
import com.reflow.model.style.StageEnum;

/**
 * 阶段流转参数。
 */
public class ChangeStageParam extends BaseParam {

    private static final long serialVersionUID = 1L;

    private StageEnum targetStage;

    /** 流转原因，落库到状态日志，用于复盘"这款为什么进清仓" */
    private String reason;

    public ChangeStageParam() {
    }

    public ChangeStageParam(StageEnum targetStage, String reason) {
        this.targetStage = targetStage;
        this.reason = reason;
    }

    public StageEnum getTargetStage() {
        return targetStage;
    }

    public void setTargetStage(StageEnum targetStage) {
        this.targetStage = targetStage;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}