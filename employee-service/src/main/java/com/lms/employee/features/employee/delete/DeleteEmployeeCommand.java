package com.lms.employee.features.employee.delete;

public class DeleteEmployeeCommand {

    private final Long targetId;

    public DeleteEmployeeCommand(Long targetId) {
        this.targetId = targetId;
    }

    public Long getTargetId() { return targetId; }
}
