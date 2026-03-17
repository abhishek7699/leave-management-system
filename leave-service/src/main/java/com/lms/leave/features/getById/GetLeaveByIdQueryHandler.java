package com.lms.leave.features.getById;

import com.lms.leave.common.exceptions.LeaveRequestNotFoundException;
import com.lms.leave.common.exceptions.UnauthorizedLeaveActionException;
import com.lms.leave.common.mediator.IQueryHandler;
import com.lms.leave.common.result.Result;
import com.lms.leave.features.LeaveResponse;
import com.lms.leave.infrastructure.persistence.entity.LeaveRequest;
import com.lms.leave.infrastructure.persistence.repository.LeaveRequestRepository;
import org.springframework.stereotype.Component;

@Component
public class GetLeaveByIdQueryHandler implements IQueryHandler<GetLeaveByIdQuery, Result<LeaveResponse>> {

    private final LeaveRequestRepository leaveRequestRepository;

    public GetLeaveByIdQueryHandler(LeaveRequestRepository leaveRequestRepository) {
        this.leaveRequestRepository = leaveRequestRepository;
    }

    @Override
    public Result<LeaveResponse> handle(GetLeaveByIdQuery query) {
        LeaveRequest leaveRequest = leaveRequestRepository.findById(query.getLeaveRequestId())
                .orElseThrow(() -> new LeaveRequestNotFoundException(query.getLeaveRequestId()));

        if ("EMPLOYEE".equals(query.getRequesterRole())) {
            if (!leaveRequest.getEmployeeId().equals(query.getRequesterId())) {
                throw new UnauthorizedLeaveActionException("Access denied");
            }
        } else if ("MANAGER".equals(query.getRequesterRole())) {
            // Manager can only view leaves from their team (managerId match)
            if (!leaveRequest.getManagerId().equals(query.getRequesterManagerId())) {
                throw new UnauthorizedLeaveActionException("Access denied");
            }
        }

        return Result.success(LeaveResponse.from(leaveRequest));
    }
}
