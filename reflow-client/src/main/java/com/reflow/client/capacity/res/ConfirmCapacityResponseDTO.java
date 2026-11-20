package com.reflow.client.capacity.res;

/**
 * 产能确认结果。
 */
public class ConfirmCapacityResponseDTO {

    private String holdId;
    private boolean confirmed;

    public String getHoldId() {
        return holdId;
    }

    public void setHoldId(String holdId) {
        this.holdId = holdId;
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public void setConfirmed(boolean confirmed) {
        this.confirmed = confirmed;
    }
}