package com.lms.employee.integration;

import com.lms.employee.infrastructure.persistence.entity.LeaveType;
import com.lms.employee.infrastructure.persistence.repository.EmployeeRepository;
import com.lms.employee.infrastructure.persistence.repository.LeaveBalanceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RegisterEmployeeIntegrationTest extends BaseIntegrationTest {

    @Autowired TestRestTemplate restTemplate;
    @Autowired EmployeeRepository employeeRepository;
    @Autowired LeaveBalanceRepository leaveBalanceRepository;

    @BeforeEach
    void setUp() {
        leaveBalanceRepository.deleteAll();
        employeeRepository.deleteAll();
        setupRedisAsEmpty();
    }

    @Test
    void register_whenValidRequest_shouldCreateEmployeeAndSeedLeaveBalances() {
        Map<String, Object> body = Map.of(
                "name", "Alice",
                "email", "alice@example.com",
                "password", "password123",
                "role", "EMPLOYEE",
                "department", "Engineering",
                "managerId", 1
        );

        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/api/employees/auth/register", body, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).containsKey("employeeId");

        Long employeeId = ((Number) response.getBody().get("employeeId")).longValue();

        assertThat(employeeRepository.findById(employeeId)).isPresent();

        var balances = leaveBalanceRepository.findAllByEmployeeId(employeeId);
        assertThat(balances).hasSize(3);
        assertThat(balances).anyMatch(b -> b.getLeaveType() == LeaveType.ANNUAL && b.getTotalDays() == 20);
        assertThat(balances).anyMatch(b -> b.getLeaveType() == LeaveType.SICK  && b.getTotalDays() == 10);
        assertThat(balances).anyMatch(b -> b.getLeaveType() == LeaveType.UNPAID);
    }

    @Test
    void register_whenEmailAlreadyExists_shouldReturn409() {
        Map<String, Object> body = Map.of(
                "name", "Bob",
                "email", "bob@example.com",
                "password", "password123",
                "role", "EMPLOYEE",
                "department", "HR"
        );

        restTemplate.postForEntity("/api/employees/auth/register", body, Map.class);
        ResponseEntity<Map> duplicate = restTemplate.postForEntity(
                "/api/employees/auth/register", body, Map.class);

        assertThat(duplicate.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(duplicate.getBody().get("message").toString())
                .contains("bob@example.com");
    }

    @Test
    void register_whenRequiredFieldMissing_shouldReturn400() {
        Map<String, Object> body = Map.of(
                "email", "incomplete@example.com",
                "password", "password123"
                // missing name, role, department
        );

        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/api/employees/auth/register", body, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
