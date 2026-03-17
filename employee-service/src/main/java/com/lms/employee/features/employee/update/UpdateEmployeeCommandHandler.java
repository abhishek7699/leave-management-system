package com.lms.employee.features.employee.update;

import com.lms.employee.common.exceptions.EmployeeNotFoundException;
import com.lms.employee.common.mediator.ICommandHandler;
import com.lms.employee.common.result.Result;
import com.lms.employee.features.employee.EmployeeResponse;
import com.lms.employee.infrastructure.persistence.entity.Employee;
import com.lms.employee.infrastructure.persistence.repository.EmployeeRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class UpdateEmployeeCommandHandler implements ICommandHandler<UpdateEmployeeCommand, Result<EmployeeResponse>> {

    private final EmployeeRepository employeeRepository;

    public UpdateEmployeeCommandHandler(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    @Override
    public Result<EmployeeResponse> handle(UpdateEmployeeCommand command) {
        // Employees may only update their own profile
        if (!command.getRequesterId().equals(command.getTargetId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only update your own profile");
        }

        Employee employee = employeeRepository.findById(command.getTargetId())
                .orElseThrow(() -> new EmployeeNotFoundException(command.getTargetId()));

        employee.setName(command.getName());
        employee.setDepartment(command.getDepartment());

        return Result.success(EmployeeResponse.from(employeeRepository.save(employee)));
    }
}
