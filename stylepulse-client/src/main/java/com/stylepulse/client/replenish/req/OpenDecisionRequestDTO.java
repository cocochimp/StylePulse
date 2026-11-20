package com.stylepulse.client.replenish.req;

import com.stylepulse.common.base.BaseParam;
import com.stylepulse.common.result.ResultDO;

/**
 * 开启返单决策请求。
 *
 * <p>{@code triggerSource} 不是装饰字段：三路触发（SCHEDULE / EVENT / MANUAL）
 * 都要落库，否则事后无法回答"这张单是谁、因为什么产生的"。
 * 幂等冲突时它也能帮我们判断是不是同一路重复触发。</p>
 */
public class OpenDecisionRequestDTO extends BaseParam {

    private static final long serialVersionUID = 1L;

    private Long styleId;

    /** 触发来源：SCHEDULE / EVENT / MANUAL */
    private String triggerSource;

    public ResultDO<Void> check() {
        if (styleId == null || styleId <= 0) {
            return ResultDO.buildFailResult("PARAM_ERROR", "styleId 非法");
        }
        if (triggerSource == null || triggerSource.isBlank()) {
            return ResultDO.buildFailResult("PARAM_ERROR", "triggerSource 不能为空");
        }
        return ResultDO.buildSuccessResult();
    }

    public Long getStyleId() {
        return styleId;
    }

    public void setStyleId(Long styleId) {
        this.styleId = styleId;
    }

    public String getTriggerSource() {
        return triggerSource;
    }

    public void setTriggerSource(String triggerSource) {
        this.triggerSource = triggerSource;
    }
}