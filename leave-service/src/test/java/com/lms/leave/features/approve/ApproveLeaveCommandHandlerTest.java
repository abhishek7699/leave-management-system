package com.lms.leave.features.approve;

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
class ApproveLeaveCommandHandlerTest {

    @Mock LeaveRequestRepository leaveRequestRepository;
    @Mock LeaveEventProducer leaveEventProducer;

    @InjectMocks ApproveLeaveCommandHandler handler;

    @Test
    void handle_whenLeaveIsPending_shouldApproveAndPublishEvent() {
        LeaveRequest lr = leaveRequest(1L, 2L, LeaveStatus.PENDING);
        when(leaveRequestRepository.findById(1L)).thenReturn(Optional.of(lr));
        when(leaveRequestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Result<LeaveResponse> result = handler.handle(new ApproveLeaveCommand(1L, 2L));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue().getStatus()).isEqualTo(LeaveStatus.APPROVED);
        verify(leaveEventProducer).publishApproved(any(), any(), any(), any());
    }

    @Test
    void handle_whenLeaveNotFound_shouldThrowLeaveRequestNotFoundException() {
        when(leaveRequestRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> handler.handle(new ApproveLeaveCommand(99L, 2L)))
                .isInstanceOf(LeaveRequestNotFoundException.class);
    }

    @Test
    void handle_whenManagerIdDoesNotMatch_shouldThrowUnauthorized() {
        LeaveRequest lr = leaveRequest(1L, 2L, LeaveStatus.PENDING);
        when(leaveRequestRepository.findById(1L)).thenReturn(Optional.of(lr));

        assertThatThrownBy(() -> handler.handle(new ApproveLeaveCommand(1L, 99L)))
                .isInstanceOf(UnauthorizedLeaveActionException.class);

        verify(leaveRequestRepository, never()).save(any());
    }

    @Test
    void handle_whenLeaveIsNotPending_shouldThrowLeaveNotPendingException() {
        LeaveRequest lr = leaveRequest(1L, 2L, LeaveStatus.APPROVED);
        when(leaveRequestRepository.findById(1L)).thenReturn(Optional.of(lr));

        assertThatThrownBy(() -> handler.handle(new ApproveLeaveCommand(1L, 2L)))
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
