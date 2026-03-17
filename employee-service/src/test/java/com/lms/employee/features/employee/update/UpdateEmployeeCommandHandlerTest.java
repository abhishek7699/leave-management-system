package com.lms.employee.features.employee.update;

import com.lms.employee.common.exceptions.EmployeeNotFoundException;
import com.lms.employee.common.result.Result;
import com.lms.employee.features.employee.EmployeeResponse;
import com.lms.employee.infrastructure.persistence.entity.Employee;
import com.lms.employee.infrastructure.persistence.entity.Role;
import com.lms.employee.infrastructure.persistence.repository.EmployeeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateEmployeeCommandHandlerTest {

    @Mock EmployeeRepository employeeRepository;

    @InjectMocks UpdateEmployeeCommandHandler handler;

    @Test
    void handle_whenEmployeeUpdatesOwnProfile_shouldReturnUpdatedResponse() {
        Employee existing = employee(1L, "Alice", "Engineering");
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(employeeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Result<EmployeeResponse> result = handler.handle(
                new UpdateEmployeeCommand(1L, 1L, "Alice Updated", "HR"));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue().getName()).isEqualTo("Alice Updated");
        assertThat(result.getValue().getDepartment()).isEqualTo("HR");
    }

    @Test
    void handle_whenEmployeeUpdatesOtherProfile_shouldThrowForbidden() {
        assertThatThrownBy(() -> handler.handle(
                new UpdateEmployeeCommand(2L, 1L, "Name", "Dept")))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void handle_whenEmployeeNotFound_shouldThrowEmployeeNotFoundException() {
        when(employeeRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> handler.handle(
                new UpdateEmployeeCommand(1L, 1L, "Name", "Dept")))
                .isInstanceOf(EmployeeNotFoundException.class);
    }

    private Employee employee(Long id, String name, String department) {
        Employee e = new Employee();
        e.setId(id);
        e.setName(name);
        e.setEmail(name.toLowerCase() + "@example.com");
        e.setRole(Role.EMPLOYEE);
        e.setDepartment(department);
        return e;
    }
}
