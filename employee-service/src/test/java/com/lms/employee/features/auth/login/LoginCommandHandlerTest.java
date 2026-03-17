package com.lms.employee.features.auth.login;

import com.lms.employee.common.exceptions.InvalidCredentialsException;
import com.lms.employee.common.result.Result;
import com.lms.employee.common.security.JwtService;
import com.lms.employee.infrastructure.persistence.entity.Employee;
import com.lms.employee.infrastructure.persistence.repository.EmployeeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoginCommandHandlerTest {

    @Mock EmployeeRepository employeeRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtService jwtService;

    @InjectMocks LoginCommandHandler handler;

    @Test
    void handle_whenCredentialsAreValid_shouldReturnJwtToken() {
        Employee employee = new Employee();
        employee.setEmail("alice@example.com");
        employee.setPassword("hashed");

        when(employeeRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(employee));
        when(passwordEncoder.matches("secret", "hashed")).thenReturn(true);
        when(jwtService.generateToken(employee)).thenReturn("jwt.token.here");

        Result<LoginResponse> result = handler.handle(new LoginCommand("alice@example.com", "secret"));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue().getToken()).isEqualTo("jwt.token.here");
        assertThat(result.getValue().getTokenType()).isEqualTo("Bearer");
    }

    @Test
    void handle_whenEmailNotFound_shouldThrowInvalidCredentialsException() {
        when(employeeRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> handler.handle(new LoginCommand("unknown@example.com", "secret")))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(passwordEncoder, never()).matches(any(), any());
    }

    @Test
    void handle_whenPasswordIsWrong_shouldThrowInvalidCredentialsException() {
        Employee employee = new Employee();
        employee.setEmail("alice@example.com");
        employee.setPassword("hashed");

        when(employeeRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(employee));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> handler.handle(new LoginCommand("alice@example.com", "wrong")))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(jwtService, never()).generateToken(any());
    }
}
