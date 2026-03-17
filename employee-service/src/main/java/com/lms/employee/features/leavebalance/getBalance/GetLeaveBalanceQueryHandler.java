package com.lms.employee.features.leavebalance.getBalance;

import com.lms.employee.common.mediator.IQueryHandler;
import com.lms.employee.common.result.Result;
import com.lms.employee.features.leavebalance.LeaveBalanceResponse;
import com.lms.employee.infrastructure.persistence.entity.LeaveBalance;
import com.lms.employee.infrastructure.persistence.repository.LeaveBalanceRepository;
import com.lms.employee.infrastructure.redis.LeaveBalanceCache;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Component
public class GetLeaveBalanceQueryHandler
        implements IQueryHandler<GetLeaveBalanceQuery, Result<List<LeaveBalanceResponse>>> {

    private final LeaveBalanceRepository leaveBalanceRepository;
    private final LeaveBalanceCache leaveBalanceCache;

    public GetLeaveBalanceQueryHandler(LeaveBalanceRepository leaveBalanceRepository,
                                       LeaveBalanceCache leaveBalanceCache) {
        this.leaveBalanceRepository = leaveBalanceRepository;
        this.leaveBalanceCache = leaveBalanceCache;
    }

    @Override
    public Result<List<LeaveBalanceResponse>> handle(GetLeaveBalanceQuery query) {
        if ("EMPLOYEE".equals(query.getRequesterRole())
                && !query.getRequesterId().equals(query.getEmployeeId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        List<LeaveBalance> balances;

        if (query.getLeaveType() != null) {
            // single type — check cache first
            Optional<Integer> cached = leaveBalanceCache.get(query.getEmployeeId(), query.getLeaveType());
            if (cached.isPresent()) {
                LeaveBalance lb = leaveBalanceRepository
                        .findByEmployeeIdAndLeaveType(query.getEmployeeId(), query.getLeaveType())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Balance not found"));
                return Result.success(List.of(LeaveBalanceResponse.from(lb)));
            }

            LeaveBalance lb = leaveBalanceRepository
                    .findByEmployeeIdAndLeaveType(query.getEmployeeId(), query.getLeaveType())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Balance not found"));

            leaveBalanceCache.put(query.getEmployeeId(), query.getLeaveType(), lb.getRemainingDays());
            balances = List.of(lb);
        } else {
            balances = leaveBalanceRepository.findAllByEmployeeId(query.getEmployeeId());
        }

        return Result.success(balances.stream().map(LeaveBalanceResponse::from).toList());
    }
}
