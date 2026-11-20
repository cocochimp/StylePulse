package com.reflow.client.replenish.req;

import com.reflow.common.base.BaseParam;
import com.reflow.common.result.ResultDO;

/**
 * 款诊断请求（规则+计算模式入参）。
 */
public class DiagnoseStyleRequestDTO extends BaseParam {

    private static final long serialVersionUID = 1L;

    private Long styleId;

    public ResultDO<Void> check() {
        if (styleId == null || styleId <= 0) {
            return ResultDO.buildFailResult("PARAM_ERROR", "styleId 非法");
        }
        return ResultDO.buildSuccessResult();
    }

    public Long getStyleId() {
        return styleId;
    }

    public void setStyleId(Long styleId) {
        this.styleId = styleId;
    }
}