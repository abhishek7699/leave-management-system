package com.lms.leave.features.approve;

public class ApproveLeaveCommand {

    private final Long leaveRequestId;
    private final Long managerId;

    public ApproveLeaveCommand(Long leaveRequestId, Long managerId) {
        this.leaveRequestId = leaveRequestId;
        this.managerId      = managerId;
    }

    public Long getLeaveRequestId() { return leaveRequestId; }
    public Long getManagerId()      { return managerId; }
}
