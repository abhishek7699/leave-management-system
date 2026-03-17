package com.lms.employee.features.auth.logout;

public class LogoutCommand {

    private final String token;

    public LogoutCommand(String token) {
        this.token = token;
    }

    public String getToken() { return token; }
}
