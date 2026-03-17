package com.lms.leave.features.apply;

import com.lms.leave.common.exceptions.OverlappingLeaveException;
import com.lms.leave.common.result.Result;
import com.lms.leave.features.LeaveResponse;
import com.lms.leave.infrastructure.client.EmployeeServiceClient;
import com.lms.leave.infrastructure.kafka.LeaveEventProducer;
import com.lms.leave.infrastructure.persistence.entity.LeaveRequest;
import com.lms.leave.infrastructure.persistence.entity.LeaveStatus;
import com.lms.leave.infrastructure.persistence.entity.LeaveType;
import com.lms.leave.infrastructure.persistence.repository.LeaveRequestRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApplyLeaveCommandHandlerTest {

    @Mock LeaveRequestRepository leaveRequestRepository;
    @Mock EmployeeServiceClient employeeServiceClient;
    @Mock LeaveEventProducer leaveEventProducer;

    @InjectMocks ApplyLeaveCommandHandler handler;

    @Test
    void handle_whenValidRequest_shouldSaveAndPublishEvent() {
        ApplyLeaveCommand command = command(LeaveType.ANNUAL,
                LocalDate.now().plusDays(1), LocalDate.now().plusDays(5));

        when(employeeServiceClient.getRemainingDays(1L, "ANNUAL", "token")).thenReturn(20);
        when(leaveRequestRepository.findOverlapping(any(), any(), any(), any())).thenReturn(List.of());

        LeaveRequest saved = leaveRequest(10L, 1L, LeaveType.ANNUAL, LeaveStatus.PENDING, 5);
        when(leaveRequestRepository.save(any())).thenReturn(saved);

        Result<LeaveResponse> result = handler.handle(command);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue().getId()).isEqualTo(10L);
        verify(leaveEventProducer).publishApplied(10L, 1L, "ANNUAL", 5);
    }

    @Test
    void handle_whenInsufficientBalance_shouldThrowBadRequest() {
        ApplyLeaveCommand command = command(LeaveType.ANNUAL,
                LocalDate.now().plusDays(1), LocalDate.now().plusDays(10));

        when(employeeServiceClient.getRemainingDays(1L, "ANNUAL", "token")).thenReturn(3);

        assertThatThrownBy(() -> handler.handle(command))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Insufficient");

        verify(leaveRequestRepository, never()).save(any());
    }

    @Test
    void handle_whenOverlappingLeaveExists_shouldThrowOverlappingLeaveException() {
        ApplyLeaveCommand command = command(LeaveType.ANNUAL,
                LocalDate.now().plusDays(1), LocalDate.now().plusDays(3));

        when(employeeServiceClient.getRemainingDays(1L, "ANNUAL", "token")).thenReturn(20);
        when(leaveRequestRepository.findOverlapping(any(), any(), any(), any()))
                .thenReturn(List.of(new LeaveRequest()));

        assertThatThrownBy(() -> handler.handle(command))
                .isInstanceOf(OverlappingLeaveException.class);
    }

    @Test
    void handle_whenLeaveTypeIsUnpaid_shouldSkipBalanceCheck() {
        ApplyLeaveCommand command = command(LeaveType.UNPAID,
                LocalDate.now().plusDays(1), LocalDate.now().plusDays(2));

        when(leaveRequestRepository.findOverlapping(any(), any(), any(), any())).thenReturn(List.of());

        LeaveRequest saved = leaveRequest(11L, 1L, LeaveType.UNPAID, LeaveStatus.PENDING, 2);
        when(leaveRequestRepository.save(any())).thenReturn(saved);

        handler.handle(command);

        verify(employeeServiceClient, never()).getRemainingDays(any(), any(), any());
    }

    @Test
    void handle_whenEndDateBeforeStartDate_shouldThrowBadRequest() {
        ApplyLeaveCommand command = command(LeaveType.ANNUAL,
                LocalDate.now().plusDays(5), LocalDate.now().plusDays(1));

        assertThatThrownBy(() -> handler.handle(command))
                .isInstanceOf(ResponseStatusException.class);
    }

    private ApplyLeaveCommand command(LeaveType type, LocalDate start, LocalDate end) {
        return new ApplyLeaveCommand(1L, 2L, type, start, end, "Medical", "token");
    }

    private LeaveRequest leaveRequest(Long id, Long employeeId, LeaveType type,
                                      LeaveStatus status, int totalDays) {
        LeaveRequest lr = new LeaveRequest();
        lr.setId(id);
        lr.setEmployeeId(employeeId);
        lr.setManagerId(2L);
        lr.setLeaveType(type);
        lr.setStartDate(LocalDate.now().plusDays(1));
        lr.setEndDate(LocalDate.now().plusDays(totalDays));
        lr.setTotalDays(totalDays);
        lr.setReason("Medical");
        lr.setStatus(status);
        return lr;
    }
}
