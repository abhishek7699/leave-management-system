package com.lms.leave.features.cancel;

import com.lms.leave.common.exceptions.LeaveAlreadyCancelledException;
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
class CancelLeaveCommandHandlerTest {

    @Mock LeaveRequestRepository leaveRequestRepository;
    @Mock LeaveEventProducer leaveEventProducer;

    @InjectMocks CancelLeaveCommandHandler handler;

    @Test
    void handle_whenLeaveIsPending_shouldCancelSuccessfully() {
        LeaveRequest lr = leaveRequest(1L, 5L, LeaveStatus.PENDING);
        when(leaveRequestRepository.findById(1L)).thenReturn(Optional.of(lr));
        when(leaveRequestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Result<LeaveResponse> result = handler.handle(new CancelLeaveCommand(1L, 5L));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue().getStatus()).isEqualTo(LeaveStatus.CANCELLED);
        verify(leaveEventProducer).publishCancelled(1L, 5L, "ANNUAL");
    }

    @Test
    void handle_whenLeaveNotFound_shouldThrowLeaveRequestNotFoundException() {
        when(leaveRequestRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> handler.handle(new CancelLeaveCommand(99L, 5L)))
                .isInstanceOf(LeaveRequestNotFoundException.class);
    }

    @Test
    void handle_whenEmployeeDoesNotOwnLeave_shouldThrowUnauthorized() {
        LeaveRequest lr = leaveRequest(1L, 5L, LeaveStatus.PENDING);
        when(leaveRequestRepository.findById(1L)).thenReturn(Optional.of(lr));

        assertThatThrownBy(() -> handler.handle(new CancelLeaveCommand(1L, 99L)))
                .isInstanceOf(UnauthorizedLeaveActionException.class);
    }

    @Test
    void handle_whenLeaveIsAlreadyCancelled_shouldThrowLeaveAlreadyCancelledException() {
        LeaveRequest lr = leaveRequest(1L, 5L, LeaveStatus.CANCELLED);
        when(leaveRequestRepository.findById(1L)).thenReturn(Optional.of(lr));

        assertThatThrownBy(() -> handler.handle(new CancelLeaveCommand(1L, 5L)))
                .isInstanceOf(LeaveAlreadyCancelledException.class);
    }

    @Test
    void handle_whenLeaveIsApproved_shouldThrowLeaveNotPendingException() {
        LeaveRequest lr = leaveRequest(1L, 5L, LeaveStatus.APPROVED);
        when(leaveRequestRepository.findById(1L)).thenReturn(Optional.of(lr));

        assertThatThrownBy(() -> handler.handle(new CancelLeaveCommand(1L, 5L)))
                .isInstanceOf(LeaveNotPendingException.class);
    }

    private LeaveRequest leaveRequest(Long id, Long employeeId, LeaveStatus status) {
        LeaveRequest lr = new LeaveRequest();
        lr.setId(id);
        lr.setEmployeeId(employeeId);
        lr.setManagerId(2L);
        lr.setLeaveType(LeaveType.ANNUAL);
        lr.setTotalDays(3);
        lr.setStatus(status);
        return lr;
    }
}
