package com.lms.leave.features.reject;

public class RejectLeaveCommand {

    private final Long leaveRequestId;
    private final Long managerId;
    private final String rejectionReason;

    public RejectLeaveCommand(Long leaveRequestId, Long managerId, String rejectionReason) {
        this.leaveRequestId  = leaveRequestId;
        this.managerId       = managerId;
        this.rejectionReason = rejectionReason;
    }

    public Long getLeaveRequestId()  { return leaveRequestId; }
    public Long getManagerId()       { return managerId; }
    public String getRejectionReason() { return rejectionReason; }
}
