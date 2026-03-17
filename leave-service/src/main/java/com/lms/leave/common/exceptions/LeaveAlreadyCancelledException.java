package com.lms.leave.common.exceptions;

public class LeaveAlreadyCancelledException extends RuntimeException {
    public LeaveAlreadyCancelledException(Long id) {
        super("Leave request " + id + " is already cancelled");
    }
}
