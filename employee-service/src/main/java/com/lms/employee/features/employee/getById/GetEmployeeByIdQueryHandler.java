package com.lms.employee.features.employee.getById;

import com.lms.employee.common.exceptions.EmployeeNotFoundException;
import com.lms.employee.common.mediator.IQueryHandler;
import com.lms.employee.common.result.Result;
import com.lms.employee.features.employee.EmployeeResponse;
import com.lms.employee.infrastructure.persistence.entity.Employee;
import com.lms.employee.infrastructure.persistence.repository.EmployeeRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class GetEmployeeByIdQueryHandler implements IQueryHandler<GetEmployeeByIdQuery, Result<EmployeeResponse>> {

    private final EmployeeRepository employeeRepository;

    public GetEmployeeByIdQueryHandler(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    @Override
    public Result<EmployeeResponse> handle(GetEmployeeByIdQuery query) {
        Employee employee = employeeRepository.findById(query.getId())
                .orElseThrow(() -> new EmployeeNotFoundException(query.getId()));

        // EMPLOYEE can only view their own profile
        if ("EMPLOYEE".equals(query.getRequesterRole())
                && !query.getRequesterId().equals(employee.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        return Result.success(EmployeeResponse.from(employee));
    }
}
