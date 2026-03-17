package com.lms.employee.features.employee.update;

import jakarta.validation.constraints.NotBlank;

public class UpdateEmployeeRequest {

    @NotBlank
    private String name;

    @NotBlank
    private String department;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
}
