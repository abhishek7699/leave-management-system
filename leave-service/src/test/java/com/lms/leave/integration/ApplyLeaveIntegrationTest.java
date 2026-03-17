package com.lms.leave.integration;

import com.lms.leave.infrastructure.persistence.entity.LeaveStatus;
import com.lms.leave.infrastructure.persistence.repository.LeaveRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.kafka.test.context.EmbeddedKafka;

import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class ApplyLeaveIntegrationTest extends BaseIntegrationTest {

    @Autowired TestRestTemplate restTemplate;
    @Autowired LeaveRequestRepository leaveRequestRepository;

    @BeforeEach
    void setUp() {
        leaveRequestRepository.deleteAll();
        // Employee has 20 ANNUAL days remaining
        when(employeeServiceClient.getRemainingDays(5L, "ANNUAL", "test-token")).thenReturn(20);
        when(employeeServiceClient.getRemainingDays(5L, "SICK", "test-token")).thenReturn(10);
    }

    @Test
    void apply_whenValidRequest_shouldCreateLeaveInPendingStatus() {
        HttpHeaders headers = employeeHeaders(5L, 2L);

        Map<String, Object> body = Map.of(
                "leaveType", "ANNUAL",
                "startDate", LocalDate.now().plusDays(1).toString(),
                "endDate",   LocalDate.now().plusDays(3).toString(),
                "reason",    "Family vacation"
        );

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/leaves", HttpMethod.POST,
                new HttpEntity<>(body, headers), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().get("status")).isEqualTo("PENDING");
        assertThat(response.getBody().get("totalDays")).isEqualTo(3);
        assertThat(response.getBody().get("employeeId")).isEqualTo(5);

        // Verify persisted to DB
        var leaves = leaveRequestRepository.findAllByEmployeeId(5L);
        assertThat(leaves).hasSize(1);
        assertThat(leaves.get(0).getStatus()).isEqualTo(LeaveStatus.PENDING);
    }

    @Test
    void apply_whenInsufficientBalance_shouldReturn400() {
        when(employeeServiceClient.getRemainingDays(5L, "ANNUAL", "test-token")).thenReturn(1);

        HttpHeaders headers = employeeHeaders(5L, 2L);
        Map<String, Object> body = Map.of(
                "leaveType", "ANNUAL",
                "startDate", LocalDate.now().plusDays(1).toString(),
                "endDate",   LocalDate.now().plusDays(10).toString(),
                "reason",    "Long vacation"
        );

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/leaves", HttpMethod.POST,
                new HttpEntity<>(body, headers), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().get("message").toString()).contains("Insufficient");
    }

    @Test
    void apply_whenDatesOverlap_shouldReturn409() {
        HttpHeaders headers = employeeHeaders(5L, 2L);
        Map<String, Object> body = Map.of(
                "leaveType", "ANNUAL",
                "startDate", LocalDate.now().plusDays(1).toString(),
                "endDate",   LocalDate.now().plusDays(3).toString(),
                "reason",    "First leave"
        );

        // Apply first time
        restTemplate.exchange("/api/leaves", HttpMethod.POST,
                new HttpEntity<>(body, headers), Map.class);

        // Apply overlapping second time
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/leaves", HttpMethod.POST,
                new HttpEntity<>(body, headers), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void apply_whenUnpaidLeave_shouldSkipBalanceCheckAndSucceed() {
        HttpHeaders headers = employeeHeaders(5L, 2L);
        Map<String, Object> body = Map.of(
                "leaveType", "UNPAID",
                "startDate", LocalDate.now().plusDays(1).toString(),
                "endDate",   LocalDate.now().plusDays(30).toString(),
                "reason",    "Sabbatical"
        );

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/leaves", HttpMethod.POST,
                new HttpEntity<>(body, headers), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().get("leaveType")).isEqualTo("UNPAID");
    }

    private HttpHeaders employeeHeaders(Long employeeId, Long managerId) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Employee-Id", String.valueOf(employeeId));
        headers.set("X-Employee-Role", "EMPLOYEE");
        headers.set("X-Manager-Id", String.valueOf(managerId));
        headers.set("Authorization", "Bearer test-token");
        return headers;
    }
}
