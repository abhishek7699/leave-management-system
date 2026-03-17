package com.lms.employee.integration;

import com.lms.employee.infrastructure.persistence.repository.EmployeeRepository;
import com.lms.employee.infrastructure.persistence.repository.LeaveBalanceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class LeaveBalanceIntegrationTest extends BaseIntegrationTest {

    @Autowired TestRestTemplate restTemplate;
    @Autowired EmployeeRepository employeeRepository;
    @Autowired LeaveBalanceRepository leaveBalanceRepository;

    private Long employeeId;
    private String jwtToken;

    @BeforeEach
    void setUp() {
        leaveBalanceRepository.deleteAll();
        employeeRepository.deleteAll();
        setupRedisAsEmpty();

        // Register and login to get a JWT
        restTemplate.postForEntity("/api/employees/auth/register", Map.of(
                "name", "Dave",
                "email", "dave@example.com",
                "password", "pass123",
                "role", "EMPLOYEE",
                "department", "QA"
        ), Map.class);

        ResponseEntity<Map> registerResponse = restTemplate.postForEntity(
                "/api/employees/auth/register", Map.of(
                        "name", "Dave",
                        "email", "dave2@example.com",
                        "password", "pass123",
                        "role", "EMPLOYEE",
                        "department", "QA"
                ), Map.class);

        // Use the second employee
        employeeId = ((Number) registerResponse.getBody().get("employeeId")).longValue();

        ResponseEntity<Map> loginResponse = restTemplate.postForEntity(
                "/api/employees/auth/login", Map.of(
                        "email", "dave2@example.com",
                        "password", "pass123"
                ), Map.class);

        jwtToken = loginResponse.getBody().get("token").toString();
    }

    @Test
    void getBalance_whenAuthenticatedEmployee_shouldReturnAllThreeBalances() {
        HttpHeaders headers = authHeaders(employeeId, "EMPLOYEE");

        ResponseEntity<List> response = restTemplate.exchange(
                "/api/employees/" + employeeId + "/balance",
                HttpMethod.GET, new HttpEntity<>(headers), List.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(3);
    }

    @Test
    void getBalanceByType_whenAuthenticatedEmployee_shouldReturnSingleBalance() {
        HttpHeaders headers = authHeaders(employeeId, "EMPLOYEE");

        ResponseEntity<List> response = restTemplate.exchange(
                "/api/employees/" + employeeId + "/balance/ANNUAL",
                HttpMethod.GET, new HttpEntity<>(headers), List.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);

        Map<String, Object> balance = (Map<String, Object>) response.getBody().get(0);
        assertThat(balance.get("leaveType")).isEqualTo("ANNUAL");
        assertThat((Integer) balance.get("totalDays")).isEqualTo(20);
        assertThat((Integer) balance.get("usedDays")).isEqualTo(0);
        assertThat((Integer) balance.get("remainingDays")).isEqualTo(20);
    }

    @Test
    void getBalance_whenEmployeeAccessesOtherBalance_shouldReturn403() {
        HttpHeaders headers = authHeaders(999L, "EMPLOYEE");  // different employee id

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/employees/" + employeeId + "/balance",
                HttpMethod.GET, new HttpEntity<>(headers), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    private HttpHeaders authHeaders(Long employeeId, String role) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Employee-Id", String.valueOf(employeeId));
        headers.set("X-Employee-Role", role);
        headers.set("Authorization", "Bearer " + jwtToken);
        return headers;
    }
}
