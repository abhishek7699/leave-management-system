package com.lms.employee.integration;

import com.lms.employee.infrastructure.persistence.repository.EmployeeRepository;
import com.lms.employee.infrastructure.persistence.repository.LeaveBalanceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class LoginIntegrationTest extends BaseIntegrationTest {

    @Autowired TestRestTemplate restTemplate;
    @Autowired EmployeeRepository employeeRepository;
    @Autowired LeaveBalanceRepository leaveBalanceRepository;

    @BeforeEach
    void setUp() {
        leaveBalanceRepository.deleteAll();
        employeeRepository.deleteAll();
        setupRedisAsEmpty();
        registerEmployee("carol@example.com", "secret123");
    }

    @Test
    void login_whenCredentialsAreValid_shouldReturnJwtToken() {
        Map<String, Object> body = Map.of(
                "email", "carol@example.com",
                "password", "secret123"
        );

        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/api/employees/auth/login", body, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsKey("token");
        assertThat(response.getBody().get("token").toString()).isNotBlank();
        assertThat(response.getBody().get("tokenType")).isEqualTo("Bearer");
    }

    @Test
    void login_whenEmailNotFound_shouldReturn401() {
        Map<String, Object> body = Map.of(
                "email", "ghost@example.com",
                "password", "secret123"
        );

        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/api/employees/auth/login", body, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void login_whenPasswordIsWrong_shouldReturn401() {
        Map<String, Object> body = Map.of(
                "email", "carol@example.com",
                "password", "wrongpassword"
        );

        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/api/employees/auth/login", body, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private void registerEmployee(String email, String password) {
        restTemplate.postForEntity("/api/employees/auth/register", Map.of(
                "name", "Carol",
                "email", email,
                "password", password,
                "role", "EMPLOYEE",
                "department", "Engineering"
        ), Map.class);
    }
}
