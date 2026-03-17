package com.lms.leave.features.approve;

import com.lms.leave.common.exceptions.LeaveNotPendingException;
import com.lms.leave.common.exceptions.LeaveRequestNotFoundException;
import com.lms.leave.common.exceptions.UnauthorizedLeaveActionException;
import com.lms.leave.common.mediator.ICommandHandler;
import com.lms.leave.common.result.Result;
import com.lms.leave.features.LeaveResponse;
import com.lms.leave.infrastructure.kafka.LeaveEventProducer;
import com.lms.leave.infrastructure.persistence.entity.LeaveRequest;
import com.lms.leave.infrastructure.persistence.entity.LeaveStatus;
import com.lms.leave.infrastructure.persistence.repository.LeaveRequestRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ApproveLeaveCommandHandler implements ICommandHandler<ApproveLeaveCommand, Result<LeaveResponse>> {

    private final LeaveRequestRepository leaveRequestRepository;
    private final LeaveEventProducer leaveEventProducer;

    public ApproveLeaveCommandHandler(LeaveRequestRepository leaveRequestRepository,
                                      LeaveEventProducer leaveEventProducer) {
        this.leaveRequestRepository = leaveRequestRepository;
        this.leaveEventProducer     = leaveEventProducer;
    }

    @Override
    @Transactional
    public Result<LeaveResponse> handle(ApproveLeaveCommand command) {
        LeaveRequest leaveRequest = leaveRequestRepository.findById(command.getLeaveRequestId())
                .orElseThrow(() -> new LeaveRequestNotFoundException(command.getLeaveRequestId()));

        if (!leaveRequest.getManagerId().equals(command.getManagerId())) {
            throw new UnauthorizedLeaveActionException("You can only approve leaves from your own department");
        }

        if (leaveRequest.getStatus() != LeaveStatus.PENDING) {
            throw new LeaveNotPendingException(command.getLeaveRequestId());
        }

        leaveRequest.setStatus(LeaveStatus.APPROVED);
        leaveRequest = leaveRequestRepository.save(leaveRequest);

        leaveEventProducer.publishApproved(
                leaveRequest.getId(), leaveRequest.getEmployeeId(),
                leaveRequest.getLeaveType().name(), leaveRequest.getTotalDays());

        return Result.success(LeaveResponse.from(leaveRequest));
    }
}
