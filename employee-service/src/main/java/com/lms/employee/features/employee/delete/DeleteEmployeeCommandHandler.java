package com.lms.employee.features.employee.delete;

import com.lms.employee.common.exceptions.EmployeeNotFoundException;
import com.lms.employee.common.mediator.ICommandHandler;
import com.lms.employee.common.result.Result;
import com.lms.employee.infrastructure.persistence.repository.EmployeeRepository;
import com.lms.employee.infrastructure.persistence.repository.LeaveBalanceRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DeleteEmployeeCommandHandler implements ICommandHandler<DeleteEmployeeCommand, Result<Void>> {

    private final EmployeeRepository employeeRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;

    public DeleteEmployeeCommandHandler(EmployeeRepository employeeRepository,
                                        LeaveBalanceRepository leaveBalanceRepository) {
        this.employeeRepository = employeeRepository;
        this.leaveBalanceRepository = leaveBalanceRepository;
    }

    @Override
    @Transactional
    public Result<Void> handle(DeleteEmployeeCommand command) {
        if (!employeeRepository.existsById(command.getTargetId())) {
            throw new EmployeeNotFoundException(command.getTargetId());
        }

        leaveBalanceRepository.findAllByEmployeeId(command.getTargetId())
                .forEach(leaveBalanceRepository::delete);

        employeeRepository.deleteById(command.getTargetId());
        return Result.success();
    }
}
