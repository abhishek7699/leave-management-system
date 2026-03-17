package com.lms.employee.common.exceptions;

public class TokenBlacklistedException extends RuntimeException {
    public TokenBlacklistedException() {
        super("Token has been invalidated");
    }
}
