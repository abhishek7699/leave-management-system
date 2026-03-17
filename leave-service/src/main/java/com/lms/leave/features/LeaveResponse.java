package com.lms.leave.features;

import com.lms.leave.infrastructure.persistence.entity.LeaveRequest;
import com.lms.leave.infrastructure.persistence.entity.LeaveStatus;
import com.lms.leave.infrastructure.persistence.entity.LeaveType;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class LeaveResponse {

    private Long id;
    private Long employeeId;
    private Long managerId;
    private LeaveType leaveType;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer totalDays;
    private String reason;
    private LeaveStatus status;
    private String rejectionReason;
    private LocalDateTime appliedAt;
    private LocalDateTime updatedAt;

    public static LeaveResponse from(LeaveRequest lr) {
        LeaveResponse r = new LeaveResponse();
        r.id              = lr.getId();
        r.employeeId      = lr.getEmployeeId();
        r.managerId       = lr.getManagerId();
        r.leaveType       = lr.getLeaveType();
        r.startDate       = lr.getStartDate();
        r.endDate         = lr.getEndDate();
        r.totalDays       = lr.getTotalDays();
        r.reason          = lr.getReason();
        r.status          = lr.getStatus();
        r.rejectionReason = lr.getRejectionReason();
        r.appliedAt       = lr.getAppliedAt();
        r.updatedAt       = lr.getUpdatedAt();
        return r;
    }

    public Long getId() { return id; }
    public Long getEmployeeId() { return employeeId; }
    public Long getManagerId() { return managerId; }
    public LeaveType getLeaveType() { return leaveType; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public Integer getTotalDays() { return totalDays; }
    public String getReason() { return reason; }
    public LeaveStatus getStatus() { return status; }
    public String getRejectionReason() { return rejectionReason; }
    public LocalDateTime getAppliedAt() { return appliedAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
