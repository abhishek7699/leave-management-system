package com.lms.leave.features.list;

public class ListLeavesByEmployeeQuery {

    private final Long employeeId;

    public ListLeavesByEmployeeQuery(Long employeeId) {
        this.employeeId = employeeId;
    }

    public Long getEmployeeId() { return employeeId; }
}
