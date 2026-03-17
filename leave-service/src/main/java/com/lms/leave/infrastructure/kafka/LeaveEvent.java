package com.lms.leave.infrastructure.kafka;

public class LeaveEvent {

    private Long leaveRequestId;
    private Long employeeId;
    private String leaveType;
    private Integer totalDays;
    private String rejectionReason;

    public LeaveEvent() {}

    private LeaveEvent(Long leaveRequestId, Long employeeId, String leaveType,
                       Integer totalDays, String rejectionReason) {
        this.leaveRequestId = leaveRequestId;
        this.employeeId = employeeId;
        this.leaveType = leaveType;
        this.totalDays = totalDays;
        this.rejectionReason = rejectionReason;
    }

    public static LeaveEvent applied(Long leaveRequestId, Long employeeId,
                                     String leaveType, Integer totalDays) {
        return new LeaveEvent(leaveRequestId, employeeId, leaveType, totalDays, null);
    }

    public static LeaveEvent approved(Long leaveRequestId, Long employeeId,
                                      String leaveType, Integer totalDays) {
        return new LeaveEvent(leaveRequestId, employeeId, leaveType, totalDays, null);
    }

    public static LeaveEvent rejected(Long leaveRequestId, Long employeeId,
                                      String leaveType, String rejectionReason) {
        return new LeaveEvent(leaveRequestId, employeeId, leaveType, null, rejectionReason);
    }

    public static LeaveEvent cancelled(Long leaveRequestId, Long employeeId, String leaveType) {
        return new LeaveEvent(leaveRequestId, employeeId, leaveType, null, null);
    }

    public Long getLeaveRequestId() { return leaveRequestId; }
    public void setLeaveRequestId(Long leaveRequestId) { this.leaveRequestId = leaveRequestId; }

    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }

    public String getLeaveType() { return leaveType; }
    public void setLeaveType(String leaveType) { this.leaveType = leaveType; }

    public Integer getTotalDays() { return totalDays; }
    public void setTotalDays(Integer totalDays) { this.totalDays = totalDays; }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
}
