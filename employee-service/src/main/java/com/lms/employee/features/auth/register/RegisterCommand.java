package com.lms.employee.features.auth.register;

import com.lms.employee.infrastructure.persistence.entity.Role;

public class RegisterCommand {

    private final String name;
    private final String email;
    private final String password;
    private final Role role;
    private final String department;
    private final Long managerId;

    public RegisterCommand(String name, String email, String password,
                           Role role, String department, Long managerId) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.role = role;
        this.department = department;
        this.managerId = managerId;
    }

    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public Role getRole() { return role; }
    public String getDepartment() { return department; }
    public Long getManagerId() { return managerId; }
}
