package com.lms.leave.features.reject;

import jakarta.validation.constraints.NotBlank;

public class RejectLeaveRequest {

    @NotBlank
    private String rejectionReason;

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
}
