package com.lms.leave.features.list;

public class ListPendingLeavesByManagerQuery {

    private final Long managerId;

    public ListPendingLeavesByManagerQuery(Long managerId) {
        this.managerId = managerId;
    }

    public Long getManagerId() { return managerId; }
}
