package com.lms.employee.features.auth.register;

import com.lms.employee.common.exceptions.EmailAlreadyExistsException;
import com.lms.employee.common.mediator.ICommandHandler;
import com.lms.employee.common.result.Result;
import com.lms.employee.infrastructure.persistence.entity.Employee;
import com.lms.employee.infrastructure.persistence.entity.LeaveBalance;
import com.lms.employee.infrastructure.persistence.entity.LeaveType;
import com.lms.employee.infrastructure.persistence.repository.EmployeeRepository;
import com.lms.employee.infrastructure.persistence.repository.LeaveBalanceRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class RegisterCommandHandler implements ICommandHandler<RegisterCommand, Result<Long>> {

    private final EmployeeRepository employeeRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final PasswordEncoder passwordEncoder;

    public RegisterCommandHandler(EmployeeRepository employeeRepository,
                                  LeaveBalanceRepository leaveBalanceRepository,
                                  PasswordEncoder passwordEncoder) {
        this.employeeRepository = employeeRepository;
        this.leaveBalanceRepository = leaveBalanceRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public Result<Long> handle(RegisterCommand command) {
        if (employeeRepository.existsByEmail(command.getEmail())) {
            throw new EmailAlreadyExistsException(command.getEmail());
        }

        Employee employee = new Employee();
        employee.setName(command.getName());
        employee.setEmail(command.getEmail());
        employee.setPassword(passwordEncoder.encode(command.getPassword()));
        employee.setRole(command.getRole());
        employee.setDepartment(command.getDepartment());
        employee.setManagerId(command.getManagerId());

        employee = employeeRepository.save(employee);

        seedLeaveBalances(employee.getId());

        return Result.success(employee.getId());
    }

    private void seedLeaveBalances(Long employeeId) {
        LeaveBalance annual = balance(employeeId, LeaveType.ANNUAL, 20);
        LeaveBalance sick   = balance(employeeId, LeaveType.SICK, 10);
        LeaveBalance unpaid = balance(employeeId, LeaveType.UNPAID, Integer.MAX_VALUE);

        leaveBalanceRepository.saveAll(List.of(annual, sick, unpaid));
    }

    private LeaveBalance balance(Long employeeId, LeaveType type, int total) {
        LeaveBalance lb = new LeaveBalance();
        lb.setEmployeeId(employeeId);
        lb.setLeaveType(type);
        lb.setTotalDays(total);
        lb.setUsedDays(0);
        return lb;
    }
}
