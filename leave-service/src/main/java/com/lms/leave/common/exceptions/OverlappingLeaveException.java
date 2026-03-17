package com.lms.leave.common.exceptions;

public class OverlappingLeaveException extends RuntimeException {
    public OverlappingLeaveException() {
        super("You already have a pending or approved leave that overlaps with the requested dates");
    }
}
