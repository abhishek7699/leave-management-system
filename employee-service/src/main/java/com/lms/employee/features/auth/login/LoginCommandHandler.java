package com.lms.employee.features.auth.login;

import com.lms.employee.common.exceptions.InvalidCredentialsException;
import com.lms.employee.common.mediator.ICommandHandler;
import com.lms.employee.common.result.Result;
import com.lms.employee.common.security.JwtService;
import com.lms.employee.infrastructure.persistence.repository.EmployeeRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class LoginCommandHandler implements ICommandHandler<LoginCommand, Result<LoginResponse>> {

    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public LoginCommandHandler(EmployeeRepository employeeRepository,
                               PasswordEncoder passwordEncoder,
                               JwtService jwtService) {
        this.employeeRepository = employeeRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Override
    public Result<LoginResponse> handle(LoginCommand command) {
        var employee = employeeRepository.findByEmail(command.getEmail())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(command.getPassword(), employee.getPassword())) {
            throw new InvalidCredentialsException();
        }

        String token = jwtService.generateToken(employee);
        return Result.success(new LoginResponse(token));
    }
}
