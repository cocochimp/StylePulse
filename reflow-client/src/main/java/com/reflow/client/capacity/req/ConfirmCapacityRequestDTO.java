package com.reflow.client.capacity.req;

import com.reflow.common.base.BaseParam;
import com.reflow.common.result.ResultDO;

/**
 * 产能确认请求：把"预占"变成"已占用"。
 *
 * <p>预占和确认分开的原因：预占发生在"决定要生产"的瞬间，确认发生在"指令真的下发成功"之后。
 * 中间这段时间如果工厂接口挂了，预占要能超时回收，否则产能会被一直挂着不放。</p>
 */
public class ConfirmCapacityRequestDTO extends BaseParam {

    private static final long serialVersionUID = 1L;

    private String holdId;

    public ResultDO<Void> check() {
        if (holdId == null || holdId.isBlank()) {
            return ResultDO.buildFailResult("PARAM_ERROR", "holdId 不能为空");
        }
        return ResultDO.buildSuccessResult();
    }

    public String getHoldId() {
        return holdId;
    }

    public void setHoldId(String holdId) {
        this.holdId = holdId;
    }
}