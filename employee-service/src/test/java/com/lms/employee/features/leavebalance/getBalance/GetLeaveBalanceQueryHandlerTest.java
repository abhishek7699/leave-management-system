package com.lms.employee.features.leavebalance.getBalance;

import com.lms.employee.common.result.Result;
import com.lms.employee.features.leavebalance.LeaveBalanceResponse;
import com.lms.employee.infrastructure.persistence.entity.LeaveBalance;
import com.lms.employee.infrastructure.persistence.entity.LeaveType;
import com.lms.employee.infrastructure.persistence.repository.LeaveBalanceRepository;
import com.lms.employee.infrastructure.redis.LeaveBalanceCache;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetLeaveBalanceQueryHandlerTest {

    @Mock LeaveBalanceRepository leaveBalanceRepository;
    @Mock LeaveBalanceCache leaveBalanceCache;

    @InjectMocks GetLeaveBalanceQueryHandler handler;

    @Test
    void handle_whenAllTypesRequested_shouldReturnAllBalances() {
        when(leaveBalanceRepository.findAllByEmployeeId(1L))
                .thenReturn(List.of(balance(1L, LeaveType.ANNUAL, 20, 5)));

        Result<List<LeaveBalanceResponse>> result = handler.handle(
                new GetLeaveBalanceQuery(1L, null, 1L, "EMPLOYEE"));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue()).hasSize(1);
        assertThat(result.getValue().get(0).getRemainingDays()).isEqualTo(15);
    }

    @Test
    void handle_whenSpecificTypeRequested_shouldReturnFromCacheIfPresent() {
        when(leaveBalanceCache.get(1L, LeaveType.ANNUAL)).thenReturn(Optional.of(15));
        when(leaveBalanceRepository.findByEmployeeIdAndLeaveType(1L, LeaveType.ANNUAL))
                .thenReturn(Optional.of(balance(1L, LeaveType.ANNUAL, 20, 5)));

        Result<List<LeaveBalanceResponse>> result = handler.handle(
                new GetLeaveBalanceQuery(1L, LeaveType.ANNUAL, 1L, "EMPLOYEE"));

        assertThat(result.isSuccess()).isTrue();
        // cache hit — should NOT put to cache again
        verify(leaveBalanceCache, never()).put(any(), any(), anyInt());
    }

    @Test
    void handle_whenCacheMiss_shouldFetchFromDbAndPopulateCache() {
        when(leaveBalanceCache.get(1L, LeaveType.SICK)).thenReturn(Optional.empty());
        when(leaveBalanceRepository.findByEmployeeIdAndLeaveType(1L, LeaveType.SICK))
                .thenReturn(Optional.of(balance(1L, LeaveType.SICK, 10, 2)));

        handler.handle(new GetLeaveBalanceQuery(1L, LeaveType.SICK, 1L, "EMPLOYEE"));

        verify(leaveBalanceCache).put(1L, LeaveType.SICK, 8);
    }

    @Test
    void handle_whenEmployeeAccessesOtherBalance_shouldThrowForbidden() {
        assertThatThrownBy(() -> handler.handle(
                new GetLeaveBalanceQuery(2L, null, 1L, "EMPLOYEE")))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void handle_whenManagerAccessesAnyBalance_shouldSucceed() {
        when(leaveBalanceRepository.findAllByEmployeeId(2L))
                .thenReturn(List.of(balance(2L, LeaveType.ANNUAL, 20, 0)));

        Result<List<LeaveBalanceResponse>> result = handler.handle(
                new GetLeaveBalanceQuery(2L, null, 10L, "MANAGER"));

        assertThat(result.isSuccess()).isTrue();
    }

    private LeaveBalance balance(Long employeeId, LeaveType type, int total, int used) {
        LeaveBalance lb = new LeaveBalance();
        lb.setEmployeeId(employeeId);
        lb.setLeaveType(type);
        lb.setTotalDays(total);
        lb.setUsedDays(used);
        return lb;
    }
}
