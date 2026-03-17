package com.lms.leave.features.cancel;

public class CancelLeaveCommand {

    private final Long leaveRequestId;
    private final Long employeeId;

    public CancelLeaveCommand(Long leaveRequestId, Long employeeId) {
        this.leaveRequestId = leaveRequestId;
        this.employeeId     = employeeId;
    }

    public Long getLeaveRequestId() { return leaveRequestId; }
    public Long getEmployeeId()     { return employeeId; }
}
