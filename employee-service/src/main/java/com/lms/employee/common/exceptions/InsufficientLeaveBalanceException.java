package com.lms.employee.common.exceptions;

public class InsufficientLeaveBalanceException extends RuntimeException {
    public InsufficientLeaveBalanceException(String leaveType, int requested, int remaining) {
        super(String.format("Insufficient %s leave balance. Requested: %d, Remaining: %d",
                leaveType, requested, remaining));
    }
}
