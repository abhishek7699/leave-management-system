package com.lms.employee.features.leavebalance.updateBalance;

import com.lms.employee.infrastructure.persistence.entity.LeaveType;

public class UpdateLeaveBalanceCommand {

    private final Long employeeId;
    private final LeaveType leaveType;
    private final int daysToDeduct;

    public UpdateLeaveBalanceCommand(Long employeeId, LeaveType leaveType, int daysToDeduct) {
        this.employeeId = employeeId;
        this.leaveType = leaveType;
        this.daysToDeduct = daysToDeduct;
    }

    public Long getEmployeeId() { return employeeId; }
    public LeaveType getLeaveType() { return leaveType; }
    public int getDaysToDeduct() { return daysToDeduct; }
}
