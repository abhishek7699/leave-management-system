package com.lms.employee.features.employee.list;

import com.lms.employee.common.mediator.IQueryHandler;
import com.lms.employee.common.result.Result;
import com.lms.employee.features.employee.EmployeeResponse;
import com.lms.employee.infrastructure.persistence.repository.EmployeeRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ListEmployeesQueryHandler implements IQueryHandler<ListEmployeesQuery, Result<List<EmployeeResponse>>> {

    private final EmployeeRepository employeeRepository;

    public ListEmployeesQueryHandler(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    @Override
    public Result<List<EmployeeResponse>> handle(ListEmployeesQuery query) {
        List<EmployeeResponse> employees = employeeRepository.findAll()
                .stream()
                .map(EmployeeResponse::from)
                .toList();
        return Result.success(employees);
    }
}
