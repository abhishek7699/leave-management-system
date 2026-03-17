package com.lms.employee.features.leavebalance.updateBalance;

import com.lms.employee.common.result.Result;
import com.lms.employee.infrastructure.persistence.entity.LeaveBalance;
import com.lms.employee.infrastructure.persistence.entity.LeaveType;
import com.lms.employee.infrastructure.persistence.repository.LeaveBalanceRepository;
import com.lms.employee.infrastructure.redis.LeaveBalanceCache;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateLeaveBalanceCommandHandlerTest {

    @Mock LeaveBalanceRepository leaveBalanceRepository;
    @Mock LeaveBalanceCache leaveBalanceCache;

    @InjectMocks UpdateLeaveBalanceCommandHandler handler;

    @Test
    void handle_whenBalanceExists_shouldDeductDaysAndEvictCache() {
        LeaveBalance lb = balance(1L, LeaveType.ANNUAL, 20, 0);
        when(leaveBalanceRepository.findByEmployeeIdAndLeaveType(1L, LeaveType.ANNUAL))
                .thenReturn(Optional.of(lb));
        when(leaveBalanceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Result<Void> result = handler.handle(
                new UpdateLeaveBalanceCommand(1L, LeaveType.ANNUAL, 5));

        assertThat(result.isSuccess()).isTrue();

        ArgumentCaptor<LeaveBalance> captor = ArgumentCaptor.forClass(LeaveBalance.class);
        verify(leaveBalanceRepository).save(captor.capture());
        assertThat(captor.getValue().getUsedDays()).isEqualTo(5);

        verify(leaveBalanceCache).evict(1L, LeaveType.ANNUAL);
    }

    @Test
    void handle_whenBalanceNotFound_shouldThrowNotFound() {
        when(leaveBalanceRepository.findByEmployeeIdAndLeaveType(1L, LeaveType.SICK))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> handler.handle(
                new UpdateLeaveBalanceCommand(1L, LeaveType.SICK, 3)))
                .isInstanceOf(ResponseStatusException.class);
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
