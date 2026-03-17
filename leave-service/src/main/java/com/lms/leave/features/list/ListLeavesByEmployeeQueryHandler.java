package com.lms.leave.features.list;

import com.lms.leave.common.mediator.IQueryHandler;
import com.lms.leave.common.result.Result;
import com.lms.leave.features.LeaveResponse;
import com.lms.leave.infrastructure.persistence.repository.LeaveRequestRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ListLeavesByEmployeeQueryHandler
        implements IQueryHandler<ListLeavesByEmployeeQuery, Result<List<LeaveResponse>>> {

    private final LeaveRequestRepository leaveRequestRepository;

    public ListLeavesByEmployeeQueryHandler(LeaveRequestRepository leaveRequestRepository) {
        this.leaveRequestRepository = leaveRequestRepository;
    }

    @Override
    public Result<List<LeaveResponse>> handle(ListLeavesByEmployeeQuery query) {
        List<LeaveResponse> leaves = leaveRequestRepository
                .findAllByEmployeeId(query.getEmployeeId())
                .stream()
                .map(LeaveResponse::from)
                .toList();
        return Result.success(leaves);
    }
}
