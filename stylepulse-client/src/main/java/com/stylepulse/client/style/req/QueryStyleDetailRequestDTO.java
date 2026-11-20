package com.stylepulse.client.style.req;

import com.stylepulse.common.base.BaseParam;
import com.stylepulse.common.result.ResultDO;

/**
 * 查询款详情请求（读模式入参）。
 */
public class QueryStyleDetailRequestDTO extends BaseParam {

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