package com.stylepulse.domain.capacity.model.param;

import com.stylepulse.common.base.BaseParam;

/**
 * 产能确认入参：把预占变成占用。
 */
public class ConfirmCapacityParam extends BaseParam {

    private static final long serialVersionUID = 1L;

    private String holdId;

    public String getHoldId() {
        return holdId;
    }

    public void setHoldId(String holdId) {
        this.holdId = holdId;
    }
}