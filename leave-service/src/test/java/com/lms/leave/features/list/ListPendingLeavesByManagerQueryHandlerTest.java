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
class ListPendingLeavesByManagerQueryHandlerTest {

    @Mock LeaveRequestRepository leaveRequestRepository;

    @InjectMocks ListPendingLeavesByManagerQueryHandler handler;

    @Test
    void handle_whenManagerHasPendingLeaves_shouldReturnOnlyPendingLeaves() {
        when(leaveRequestRepository.findAllByManagerIdAndStatus(2L, LeaveStatus.PENDING))
                .thenReturn(List.of(leaveRequest(1L, 2L), leaveRequest(2L, 2L)));

        Result<List<LeaveResponse>> result = handler.handle(
                new ListPendingLeavesByManagerQuery(2L));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue()).hasSize(2);
        assertThat(result.getValue()).allMatch(r -> r.getStatus() == LeaveStatus.PENDING);
    }

    @Test
    void handle_whenNoPendingLeaves_shouldReturnEmptyList() {
        when(leaveRequestRepository.findAllByManagerIdAndStatus(2L, LeaveStatus.PENDING))
                .thenReturn(List.of());

        Result<List<LeaveResponse>> result = handler.handle(
                new ListPendingLeavesByManagerQuery(2L));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue()).isEmpty();
    }

    private LeaveRequest leaveRequest(Long id, Long managerId) {
        LeaveRequest lr = new LeaveRequest();
        lr.setId(id);
        lr.setEmployeeId(5L);
        lr.setManagerId(managerId);
        lr.setLeaveType(LeaveType.SICK);
        lr.setTotalDays(2);
        lr.setStatus(LeaveStatus.PENDING);
        return lr;
    }
}
