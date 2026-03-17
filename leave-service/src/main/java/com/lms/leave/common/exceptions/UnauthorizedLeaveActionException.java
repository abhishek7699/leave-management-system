package com.lms.leave.common.exceptions;

public class UnauthorizedLeaveActionException extends RuntimeException {
    public UnauthorizedLeaveActionException(String message) {
        super(message);
    }
}
