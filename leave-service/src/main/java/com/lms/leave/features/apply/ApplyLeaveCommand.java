package com.lms.leave.features.apply;

import com.lms.leave.infrastructure.persistence.entity.LeaveType;

import java.time.LocalDate;

public class ApplyLeaveCommand {

    private final Long employeeId;
    private final Long managerId;
    private final LeaveType leaveType;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final String reason;
    private final String bearerToken;

    public ApplyLeaveCommand(Long employeeId, Long managerId, LeaveType leaveType,
                             LocalDate startDate, LocalDate endDate,
                             String reason, String bearerToken) {
        this.employeeId  = employeeId;
        this.managerId   = managerId;
        this.leaveType   = leaveType;
        this.startDate   = startDate;
        this.endDate     = endDate;
        this.reason      = reason;
        this.bearerToken = bearerToken;
    }

    public Long getEmployeeId()  { return employeeId; }
    public Long getManagerId()   { return managerId; }
    public LeaveType getLeaveType() { return leaveType; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate()   { return endDate; }
    public String getReason()       { return reason; }
    public String getBearerToken()  { return bearerToken; }
}
