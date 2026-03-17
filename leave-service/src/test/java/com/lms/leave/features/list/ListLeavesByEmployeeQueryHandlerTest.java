package com.lms.leave.features.list;

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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListLeavesByEmployeeQueryHandlerTest {

    @Mock LeaveRequestRepository leaveRequestRepository;

    @InjectMocks ListLeavesByEmployeeQueryHandler handler;

    @Test
    void handle_whenEmployeeHasLeaves_shouldReturnAllLeaves() {
        when(leaveRequestRepository.findAllByEmployeeId(5L))
                .thenReturn(List.of(leaveRequest(1L, 5L), leaveRequest(2L, 5L)));

        Result<List<LeaveResponse>> result = handler.handle(new ListLeavesByEmployeeQuery(5L));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue()).hasSize(2);
    }

    @Test
    void handle_whenEmployeeHasNoLeaves_shouldReturnEmptyList() {
        when(leaveRequestRepository.findAllByEmployeeId(5L)).thenReturn(List.of());

        Result<List<LeaveResponse>> result = handler.handle(new ListLeavesByEmployeeQuery(5L));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue()).isEmpty();
    }

    private LeaveRequest leaveRequest(Long id, Long employeeId) {
        LeaveRequest lr = new LeaveRequest();
        lr.setId(id);
        lr.setEmployeeId(employeeId);
        lr.setManagerId(2L);
        lr.setLeaveType(LeaveType.ANNUAL);
        lr.setTotalDays(3);
        lr.setStatus(LeaveStatus.PENDING);
        return lr;
    }
}
