package com.reflow.domain.capacity.model.result;

import com.reflow.common.base.BaseResult;

/**
 * 产能预占结果。
 */
public class ReserveCapacityResult extends BaseResult {

    private static final long serialVersionUID = 1L;

    private final boolean success;
    private final String holdId;
    private final int remainQty;
    private final String reason;

    private ReserveCapacityResult(boolean success, String holdId, int remainQty, String reason) {
        this.success = success;
        this.holdId = holdId;
        this.remainQty = remainQty;
        this.reason = reason;
    }

    public static ReserveCapacityResult ok(String holdId, int remainQty) {
        return new ReserveCapacityResult(true, holdId, remainQty, null);
    }

    public static ReserveCapacityResult rejected(String reason, int remainQty) {
        return new ReserveCapacityResult(false, null, remainQty, reason);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getHoldId() {
        return holdId;
    }

    public int getRemainQty() {
        return remainQty;
    }

    public String getReason() {
        return reason;
    }
}