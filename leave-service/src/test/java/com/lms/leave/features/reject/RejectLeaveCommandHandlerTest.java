package com.lms.leave.features.reject;

import com.lms.leave.common.exceptions.LeaveNotPendingException;
import com.lms.leave.common.exceptions.LeaveRequestNotFoundException;
import com.lms.leave.common.exceptions.UnauthorizedLeaveActionException;
import com.lms.leave.common.result.Result;
import com.lms.leave.features.LeaveResponse;
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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RejectLeaveCommandHandlerTest {

    @Mock LeaveRequestRepository leaveRequestRepository;
    @Mock LeaveEventProducer leaveEventProducer;

    @InjectMocks RejectLeaveCommandHandler handler;

    @Test
    void handle_whenLeaveIsPending_shouldRejectWithReasonAndPublishEvent() {
        LeaveRequest lr = leaveRequest(1L, 2L, LeaveStatus.PENDING);
        when(leaveRequestRepository.findById(1L)).thenReturn(Optional.of(lr));
        when(leaveRequestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Result<LeaveResponse> result = handler.handle(
                new RejectLeaveCommand(1L, 2L, "Team at full capacity"));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue().getStatus()).isEqualTo(LeaveStatus.REJECTED);
        assertThat(result.getValue().getRejectionReason()).isEqualTo("Team at full capacity");
        verify(leaveEventProducer).publishRejected(1L, 5L, "ANNUAL", "Team at full capacity");
    }

    @Test
    void handle_whenLeaveNotFound_shouldThrowLeaveRequestNotFoundException() {
        when(leaveRequestRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> handler.handle(new RejectLeaveCommand(99L, 2L, "reason")))
                .isInstanceOf(LeaveRequestNotFoundException.class);
    }

    @Test
    void handle_whenManagerIdDoesNotMatch_shouldThrowUnauthorized() {
        LeaveRequest lr = leaveRequest(1L, 2L, LeaveStatus.PENDING);
        when(leaveRequestRepository.findById(1L)).thenReturn(Optional.of(lr));

        assertThatThrownBy(() -> handler.handle(new RejectLeaveCommand(1L, 99L, "reason")))
                .isInstanceOf(UnauthorizedLeaveActionException.class);
    }

    @Test
    void handle_whenLeaveIsNotPending_shouldThrowLeaveNotPendingException() {
        LeaveRequest lr = leaveRequest(1L, 2L, LeaveStatus.CANCELLED);
        when(leaveRequestRepository.findById(1L)).thenReturn(Optional.of(lr));

        assertThatThrownBy(() -> handler.handle(new RejectLeaveCommand(1L, 2L, "reason")))
                .isInstanceOf(LeaveNotPendingException.class);
    }

    private LeaveRequest leaveRequest(Long id, Long managerId, LeaveStatus status) {
        LeaveRequest lr = new LeaveRequest();
        lr.setId(id);
        lr.setEmployeeId(5L);
        lr.setManagerId(managerId);
        lr.setLeaveType(LeaveType.ANNUAL);
        lr.setTotalDays(3);
        lr.setStatus(status);
        return lr;
    }
}
