package com.lms.leave.common.exceptions;

public class LeaveRequestNotFoundException extends RuntimeException {
    public LeaveRequestNotFoundException(Long id) {
        super("Leave request not found with id: " + id);
    }
}
