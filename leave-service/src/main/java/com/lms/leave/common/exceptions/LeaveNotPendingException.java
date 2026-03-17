package com.lms.leave.common.exceptions;

public class LeaveNotPendingException extends RuntimeException {
    public LeaveNotPendingException(Long id) {
        super("Leave request " + id + " is not in PENDING status");
    }
}
