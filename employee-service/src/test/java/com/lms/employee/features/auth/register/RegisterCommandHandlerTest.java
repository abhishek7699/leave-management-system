package com.lms.employee.features.auth.register;

import com.lms.employee.common.exceptions.EmailAlreadyExistsException;
import com.lms.employee.common.result.Result;
import com.lms.employee.infrastructure.persistence.entity.Employee;
import com.lms.employee.infrastructure.persistence.entity.Role;
import com.lms.employee.infrastructure.persistence.repository.EmployeeRepository;
import com.lms.employee.infrastructure.persistence.repository.LeaveBalanceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegisterCommandHandlerTest {

    @Mock EmployeeRepository employeeRepository;
    @Mock LeaveBalanceRepository leaveBalanceRepository;
    @Mock PasswordEncoder passwordEncoder;

    @InjectMocks RegisterCommandHandler handler;

    @Test
    void handle_whenEmailIsNew_shouldSaveEmployeeAndSeedBalances() {
        RegisterCommand command = new RegisterCommand(
                "Alice", "alice@example.com", "secret", Role.EMPLOYEE, "Engineering", 2L);

        when(employeeRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(passwordEncoder.encode("secret")).thenReturn("hashed");

        Employee saved = new Employee();
        saved.setId(1L);
        when(employeeRepository.save(any(Employee.class))).thenReturn(saved);

        Result<Long> result = handler.handle(command);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue()).isEqualTo(1L);

        // 3 leave balance rows seeded (ANNUAL, SICK, UNPAID)
        ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        verify(leaveBalanceRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(3);
    }

    @Test
    void handle_whenEmailAlreadyExists_shouldThrowEmailAlreadyExistsException() {
        RegisterCommand command = new RegisterCommand(
                "Bob", "bob@example.com", "secret", Role.EMPLOYEE, "Engineering", 2L);

        when(employeeRepository.existsByEmail("bob@example.com")).thenReturn(true);

        assertThatThrownBy(() -> handler.handle(command))
                .isInstanceOf(EmailAlreadyExistsException.class)
                .hasMessageContaining("bob@example.com");

        verify(employeeRepository, never()).save(any());
        verify(leaveBalanceRepository, never()).saveAll(any());
    }

    @Test
    void handle_whenRegistering_shouldHashPassword() {
        RegisterCommand command = new RegisterCommand(
                "Carol", "carol@example.com", "plaintext", Role.MANAGER, "HR", null);

        when(employeeRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode("plaintext")).thenReturn("bcrypt-hash");

        Employee saved = new Employee();
        saved.setId(3L);
        when(employeeRepository.save(any(Employee.class))).thenReturn(saved);

        handler.handle(command);

        ArgumentCaptor<Employee> captor = ArgumentCaptor.forClass(Employee.class);
        verify(employeeRepository).save(captor.capture());
        assertThat(captor.getValue().getPassword()).isEqualTo("bcrypt-hash");
    }
}
