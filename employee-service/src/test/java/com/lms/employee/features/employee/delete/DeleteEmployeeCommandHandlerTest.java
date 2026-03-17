package com.lms.employee.features.employee.delete;

import com.lms.employee.common.exceptions.EmployeeNotFoundException;
import com.lms.employee.common.result.Result;
import com.lms.employee.infrastructure.persistence.entity.LeaveBalance;
import com.lms.employee.infrastructure.persistence.entity.LeaveType;
import com.lms.employee.infrastructure.persistence.repository.EmployeeRepository;
import com.lms.employee.infrastructure.persistence.repository.LeaveBalanceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeleteEmployeeCommandHandlerTest {

    @Mock EmployeeRepository employeeRepository;
    @Mock LeaveBalanceRepository leaveBalanceRepository;

    @InjectMocks DeleteEmployeeCommandHandler handler;

    @Test
    void handle_whenEmployeeExists_shouldDeleteEmployeeAndBalances() {
        LeaveBalance annual = balance(1L, LeaveType.ANNUAL);
        LeaveBalance sick   = balance(1L, LeaveType.SICK);

        when(employeeRepository.existsById(1L)).thenReturn(true);
        when(leaveBalanceRepository.findAllByEmployeeId(1L)).thenReturn(List.of(annual, sick));

        Result<Void> result = handler.handle(new DeleteEmployeeCommand(1L));

        assertThat(result.isSuccess()).isTrue();
        verify(leaveBalanceRepository).delete(annual);
        verify(leaveBalanceRepository).delete(sick);
        verify(employeeRepository).deleteById(1L);
    }

    @Test
    void handle_whenEmployeeNotFound_shouldThrowEmployeeNotFoundException() {
        when(employeeRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> handler.handle(new DeleteEmployeeCommand(99L)))
                .isInstanceOf(EmployeeNotFoundException.class)
                .hasMessageContaining("99");

        verify(employeeRepository, never()).deleteById(any());
    }

    private LeaveBalance balance(Long employeeId, LeaveType type) {
        LeaveBalance lb = new LeaveBalance();
        lb.setEmployeeId(employeeId);
        lb.setLeaveType(type);
        lb.setTotalDays(20);
        lb.setUsedDays(0);
        return lb;
    }
}
