package com.lms.employee.features.employee;

import com.lms.employee.infrastructure.persistence.entity.Employee;
import com.lms.employee.infrastructure.persistence.entity.Role;

import java.time.LocalDateTime;

public class EmployeeResponse {

    private Long id;
    private String name;
    private String email;
    private Role role;
    private String department;
    private Long managerId;
    private LocalDateTime createdAt;

    public static EmployeeResponse from(Employee employee) {
        EmployeeResponse r = new EmployeeResponse();
        r.id = employee.getId();
        r.name = employee.getName();
        r.email = employee.getEmail();
        r.role = employee.getRole();
        r.department = employee.getDepartment();
        r.managerId = employee.getManagerId();
        r.createdAt = employee.getCreatedAt();
        return r;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public Role getRole() { return role; }
    public String getDepartment() { return department; }
    public Long getManagerId() { return managerId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
