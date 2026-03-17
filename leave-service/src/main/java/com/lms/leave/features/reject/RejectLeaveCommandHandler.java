package com.lms.leave.features.reject;

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
public class RejectLeaveCommandHandler implements ICommandHandler<RejectLeaveCommand, Result<LeaveResponse>> {

    private final LeaveRequestRepository leaveRequestRepository;
    private final LeaveEventProducer leaveEventProducer;

    public RejectLeaveCommandHandler(LeaveRequestRepository leaveRequestRepository,
                                     LeaveEventProducer leaveEventProducer) {
        this.leaveRequestRepository = leaveRequestRepository;
        this.leaveEventProducer     = leaveEventProducer;
    }

    @Override
    @Transactional
    public Result<LeaveResponse> handle(RejectLeaveCommand command) {
        LeaveRequest leaveRequest = leaveRequestRepository.findById(command.getLeaveRequestId())
                .orElseThrow(() -> new LeaveRequestNotFoundException(command.getLeaveRequestId()));

        if (!leaveRequest.getManagerId().equals(command.getManagerId())) {
            throw new UnauthorizedLeaveActionException("You can only reject leaves from your own department");
        }

        if (leaveRequest.getStatus() != LeaveStatus.PENDING) {
            throw new LeaveNotPendingException(command.getLeaveRequestId());
        }

        leaveRequest.setStatus(LeaveStatus.REJECTED);
        leaveRequest.setRejectionReason(command.getRejectionReason());
        leaveRequest = leaveRequestRepository.save(leaveRequest);

        leaveEventProducer.publishRejected(
                leaveRequest.getId(), leaveRequest.getEmployeeId(),
                leaveRequest.getLeaveType().name(), command.getRejectionReason());

        return Result.success(LeaveResponse.from(leaveRequest));
    }
}
