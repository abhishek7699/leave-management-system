package com.lms.employee.features.employee.update;

public class UpdateEmployeeCommand {

    private final Long targetId;
    private final Long requesterId;
    private final String name;
    private final String department;

    public UpdateEmployeeCommand(Long targetId, Long requesterId, String name, String department) {
        this.targetId = targetId;
        this.requesterId = requesterId;
        this.name = name;
        this.department = department;
    }

    public Long getTargetId() { return targetId; }
    public Long getRequesterId() { return requesterId; }
    public String getName() { return name; }
    public String getDepartment() { return department; }
}
