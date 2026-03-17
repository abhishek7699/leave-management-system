package com.lms.leave.integration;

import com.lms.leave.infrastructure.persistence.entity.LeaveRequest;
import com.lms.leave.infrastructure.persistence.entity.LeaveStatus;
import com.lms.leave.infrastructure.persistence.entity.LeaveType;
import com.lms.leave.infrastructure.persistence.repository.LeaveRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ApproveLeaveIntegrationTest extends BaseIntegrationTest {

    @Autowired TestRestTemplate restTemplate;
    @Autowired LeaveRequestRepository leaveRequestRepository;

    private LeaveRequest pendingLeave;

    @BeforeEach
    void setUp() {
        leaveRequestRepository.deleteAll();
        pendingLeave = leaveRequestRepository.save(pendingLeaveRequest(5L, 2L));
    }

    @Test
    void approve_whenManagerApprovesOwnTeamLeave_shouldSetStatusApproved() {
        HttpHeaders headers = managerHeaders(2L);

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/leaves/" + pendingLeave.getId() + "/approve",
                HttpMethod.PUT, new HttpEntity<>(headers), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("status")).isEqualTo("APPROVED");

        LeaveRequest updated = leaveRequestRepository.findById(pendingLeave.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(LeaveStatus.APPROVED);
    }

    @Test
    void approve_whenManagerApprovesOtherTeamLeave_shouldReturn403() {
        HttpHeaders headers = managerHeaders(99L); // wrong manager

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/leaves/" + pendingLeave.getId() + "/approve",
                HttpMethod.PUT, new HttpEntity<>(headers), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void approve_whenLeaveAlreadyApproved_shouldReturn400() {
        // Approve once
        restTemplate.exchange(
                "/api/leaves/" + pendingLeave.getId() + "/approve",
                HttpMethod.PUT, new HttpEntity<>(managerHeaders(2L)), Map.class);

        // Attempt to approve again
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/leaves/" + pendingLeave.getId() + "/approve",
                HttpMethod.PUT, new HttpEntity<>(managerHeaders(2L)), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void approve_whenLeaveNotFound_shouldReturn404() {
        HttpHeaders headers = managerHeaders(2L);

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/leaves/999999/approve",
                HttpMethod.PUT, new HttpEntity<>(headers), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    private LeaveRequest pendingLeaveRequest(Long employeeId, Long managerId) {
        LeaveRequest lr = new LeaveRequest();
        lr.setEmployeeId(employeeId);
        lr.setManagerId(managerId);
        lr.setLeaveType(LeaveType.ANNUAL);
        lr.setStartDate(LocalDate.now().plusDays(1));
        lr.setEndDate(LocalDate.now().plusDays(3));
        lr.setReason("Vacation");
        lr.setStatus(LeaveStatus.PENDING);
        return lr;
    }

    private HttpHeaders managerHeaders(Long managerId) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Employee-Id", String.valueOf(managerId));
        headers.set("X-Employee-Role", "MANAGER");
        return headers;
    }
}
