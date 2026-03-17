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

class CancelLeaveIntegrationTest extends BaseIntegrationTest {

    @Autowired TestRestTemplate restTemplate;
    @Autowired LeaveRequestRepository leaveRequestRepository;

    private LeaveRequest pendingLeave;

    @BeforeEach
    void setUp() {
        leaveRequestRepository.deleteAll();
        pendingLeave = leaveRequestRepository.save(pendingLeaveRequest(5L, 2L));
    }

    @Test
    void cancel_whenEmployeeCancelsOwnPendingLeave_shouldSetStatusCancelled() {
        HttpHeaders headers = employeeHeaders(5L);

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/leaves/" + pendingLeave.getId() + "/cancel",
                HttpMethod.PUT, new HttpEntity<>(headers), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("status")).isEqualTo("CANCELLED");

        LeaveRequest updated = leaveRequestRepository.findById(pendingLeave.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(LeaveStatus.CANCELLED);
    }

    @Test
    void cancel_whenEmployeeCancelsOtherEmployeeLeave_shouldReturn403() {
        HttpHeaders headers = employeeHeaders(99L); // different employee

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/leaves/" + pendingLeave.getId() + "/cancel",
                HttpMethod.PUT, new HttpEntity<>(headers), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void cancel_whenLeaveAlreadyCancelled_shouldReturn400() {
        // Cancel once
        restTemplate.exchange(
                "/api/leaves/" + pendingLeave.getId() + "/cancel",
                HttpMethod.PUT, new HttpEntity<>(employeeHeaders(5L)), Map.class);

        // Cancel again
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/leaves/" + pendingLeave.getId() + "/cancel",
                HttpMethod.PUT, new HttpEntity<>(employeeHeaders(5L)), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void cancel_whenLeaveIsApproved_shouldReturn400() {
        LeaveRequest approvedLeave = leaveRequestRepository.save(
                approvedLeaveRequest(5L, 2L));

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/leaves/" + approvedLeave.getId() + "/cancel",
                HttpMethod.PUT, new HttpEntity<>(employeeHeaders(5L)), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void cancel_whenLeaveNotFound_shouldReturn404() {
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/leaves/999999/cancel",
                HttpMethod.PUT, new HttpEntity<>(employeeHeaders(5L)), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    private LeaveRequest pendingLeaveRequest(Long employeeId, Long managerId) {
        LeaveRequest lr = new LeaveRequest();
        lr.setEmployeeId(employeeId);
        lr.setManagerId(managerId);
        lr.setLeaveType(LeaveType.SICK);
        lr.setStartDate(LocalDate.now().plusDays(1));
        lr.setEndDate(LocalDate.now().plusDays(2));
        lr.setReason("Sick");
        lr.setStatus(LeaveStatus.PENDING);
        return lr;
    }

    private LeaveRequest approvedLeaveRequest(Long employeeId, Long managerId) {
        LeaveRequest lr = new LeaveRequest();
        lr.setEmployeeId(employeeId);
        lr.setManagerId(managerId);
        lr.setLeaveType(LeaveType.ANNUAL);
        lr.setStartDate(LocalDate.now().plusDays(5));
        lr.setEndDate(LocalDate.now().plusDays(7));
        lr.setReason("Holiday");
        lr.setStatus(LeaveStatus.APPROVED);
        return lr;
    }

    private HttpHeaders employeeHeaders(Long employeeId) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Employee-Id", String.valueOf(employeeId));
        headers.set("X-Employee-Role", "EMPLOYEE");
        return headers;
    }
}
