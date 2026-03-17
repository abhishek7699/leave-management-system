package com.lms.leave.features.getById;

import com.lms.leave.common.exceptions.LeaveRequestNotFoundException;
import com.lms.leave.common.exceptions.UnauthorizedLeaveActionException;
import com.lms.leave.common.result.Result;
import com.lms.leave.features.LeaveResponse;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetLeaveByIdQueryHandlerTest {

    @Mock LeaveRequestRepository leaveRequestRepository;

    @InjectMocks GetLeaveByIdQueryHandler handler;

    @Test
    void handle_whenEmployeeRequestsOwnLeave_shouldReturnResponse() {
        LeaveRequest lr = leaveRequest(1L, 5L, 2L);
        when(leaveRequestRepository.findById(1L)).thenReturn(Optional.of(lr));

        Result<LeaveResponse> result = handler.handle(
                new GetLeaveByIdQuery(1L, 5L, "EMPLOYEE", null));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue().getEmployeeId()).isEqualTo(5L);
    }

    @Test
    void handle_whenEmployeeRequestsOtherLeave_shouldThrowUnauthorized() {
        LeaveRequest lr = leaveRequest(1L, 5L, 2L);
        when(leaveRequestRepository.findById(1L)).thenReturn(Optional.of(lr));

        assertThatThrownBy(() -> handler.handle(
                new GetLeaveByIdQuery(1L, 99L, "EMPLOYEE", null)))
                .isInstanceOf(UnauthorizedLeaveActionException.class);
    }

    @Test
    void handle_whenManagerRequestsTeamLeave_shouldReturnResponse() {
        LeaveRequest lr = leaveRequest(1L, 5L, 2L);
        when(leaveRequestRepository.findById(1L)).thenReturn(Optional.of(lr));

        Result<LeaveResponse> result = handler.handle(
                new GetLeaveByIdQuery(1L, 2L, "MANAGER", 2L));

        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    void handle_whenManagerRequestsOtherTeamLeave_shouldThrowUnauthorized() {
        LeaveRequest lr = leaveRequest(1L, 5L, 2L);
        when(leaveRequestRepository.findById(1L)).thenReturn(Optional.of(lr));

        assertThatThrownBy(() -> handler.handle(
                new GetLeaveByIdQuery(1L, 99L, "MANAGER", 99L)))
                .isInstanceOf(UnauthorizedLeaveActionException.class);
    }

    @Test
    void handle_whenLeaveNotFound_shouldThrowLeaveRequestNotFoundException() {
        when(leaveRequestRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> handler.handle(
                new GetLeaveByIdQuery(99L, 5L, "EMPLOYEE", null)))
                .isInstanceOf(LeaveRequestNotFoundException.class);
    }

    private LeaveRequest leaveRequest(Long id, Long employeeId, Long managerId) {
        LeaveRequest lr = new LeaveRequest();
        lr.setId(id);
        lr.setEmployeeId(employeeId);
        lr.setManagerId(managerId);
        lr.setLeaveType(LeaveType.ANNUAL);
        lr.setTotalDays(3);
        lr.setStatus(LeaveStatus.PENDING);
        return lr;
    }
}
