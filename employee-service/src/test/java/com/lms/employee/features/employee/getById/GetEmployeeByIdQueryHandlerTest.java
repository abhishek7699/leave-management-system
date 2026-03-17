package com.lms.employee.features.employee.getById;

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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetEmployeeByIdQueryHandlerTest {

    @Mock EmployeeRepository employeeRepository;

    @InjectMocks GetEmployeeByIdQueryHandler handler;

    @Test
    void handle_whenEmployeeExists_shouldReturnEmployeeResponse() {
        Employee employee = employee(1L, "Alice", Role.EMPLOYEE);
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));

        Result<EmployeeResponse> result = handler.handle(
                new GetEmployeeByIdQuery(1L, 1L, "EMPLOYEE"));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue().getName()).isEqualTo("Alice");
    }

    @Test
    void handle_whenEmployeeNotFound_shouldThrowEmployeeNotFoundException() {
        when(employeeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> handler.handle(new GetEmployeeByIdQuery(99L, 1L, "MANAGER")))
                .isInstanceOf(EmployeeNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void handle_whenEmployeeAccessesOtherProfile_shouldThrowForbidden() {
        Employee employee = employee(2L, "Bob", Role.EMPLOYEE);
        when(employeeRepository.findById(2L)).thenReturn(Optional.of(employee));

        assertThatThrownBy(() -> handler.handle(
                new GetEmployeeByIdQuery(2L, 1L, "EMPLOYEE")))  // requester=1, target=2
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void handle_whenManagerAccessesAnyEmployee_shouldReturnResponse() {
        Employee employee = employee(2L, "Bob", Role.EMPLOYEE);
        when(employeeRepository.findById(2L)).thenReturn(Optional.of(employee));

        Result<EmployeeResponse> result = handler.handle(
                new GetEmployeeByIdQuery(2L, 10L, "MANAGER"));

        assertThat(result.isSuccess()).isTrue();
    }

    private Employee employee(Long id, String name, Role role) {
        Employee e = new Employee();
        e.setId(id);
        e.setName(name);
        e.setEmail(name.toLowerCase() + "@example.com");
        e.setRole(role);
        e.setDepartment("Engineering");
        return e;
    }
}
