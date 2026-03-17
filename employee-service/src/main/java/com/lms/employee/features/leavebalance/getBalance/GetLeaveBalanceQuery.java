package com.lms.employee.features.leavebalance.getBalance;

import com.lms.employee.infrastructure.persistence.entity.LeaveType;

public class GetLeaveBalanceQuery {

    private final Long employeeId;
    private final LeaveType leaveType;   // null = fetch all types
    private final Long requesterId;
    private final String requesterRole;

    public GetLeaveBalanceQuery(Long employeeId, LeaveType leaveType,
                                Long requesterId, String requesterRole) {
        this.employeeId = employeeId;
        this.leaveType = leaveType;
        this.requesterId = requesterId;
        this.requesterRole = requesterRole;
    }

    public Long getEmployeeId() { return employeeId; }
    public LeaveType getLeaveType() { return leaveType; }
    public Long getRequesterId() { return requesterId; }
    public String getRequesterRole() { return requesterRole; }
}
