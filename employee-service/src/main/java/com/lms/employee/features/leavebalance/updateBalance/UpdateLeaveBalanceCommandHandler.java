package com.lms.employee.features.leavebalance.updateBalance;

import com.lms.employee.common.mediator.ICommandHandler;
import com.lms.employee.common.result.Result;
import com.lms.employee.infrastructure.persistence.entity.LeaveBalance;
import com.lms.employee.infrastructure.persistence.repository.LeaveBalanceRepository;
import com.lms.employee.infrastructure.redis.LeaveBalanceCache;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Component
public class UpdateLeaveBalanceCommandHandler
        implements ICommandHandler<UpdateLeaveBalanceCommand, Result<Void>> {

    private final LeaveBalanceRepository leaveBalanceRepository;
    private final LeaveBalanceCache leaveBalanceCache;

    public UpdateLeaveBalanceCommandHandler(LeaveBalanceRepository leaveBalanceRepository,
                                            LeaveBalanceCache leaveBalanceCache) {
        this.leaveBalanceRepository = leaveBalanceRepository;
        this.leaveBalanceCache = leaveBalanceCache;
    }

    @Override
    @Transactional
    public Result<Void> handle(UpdateLeaveBalanceCommand command) {
        LeaveBalance balance = leaveBalanceRepository
                .findByEmployeeIdAndLeaveType(command.getEmployeeId(), command.getLeaveType())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Leave balance not found"));

        balance.setUsedDays(balance.getUsedDays() + command.getDaysToDeduct());
        leaveBalanceRepository.save(balance);
        leaveBalanceCache.evict(command.getEmployeeId(), command.getLeaveType());

        return Result.success();
    }
}
