package com.lms.employee.features.leavebalance;

import com.lms.employee.infrastructure.persistence.entity.LeaveBalance;
import com.lms.employee.infrastructure.persistence.entity.LeaveType;

public class LeaveBalanceResponse {

    private Long employeeId;
    private LeaveType leaveType;
    private Integer totalDays;
    private Integer usedDays;
    private Integer remainingDays;

    public static LeaveBalanceResponse from(LeaveBalance lb) {
        LeaveBalanceResponse r = new LeaveBalanceResponse();
        r.employeeId = lb.getEmployeeId();
        r.leaveType = lb.getLeaveType();
        r.totalDays = lb.getTotalDays();
        r.usedDays = lb.getUsedDays();
        r.remainingDays = lb.getRemainingDays();
        return r;
    }

    public Long getEmployeeId() { return employeeId; }
    public LeaveType getLeaveType() { return leaveType; }
    public Integer getTotalDays() { return totalDays; }
    public Integer getUsedDays() { return usedDays; }
    public Integer getRemainingDays() { return remainingDays; }
}
