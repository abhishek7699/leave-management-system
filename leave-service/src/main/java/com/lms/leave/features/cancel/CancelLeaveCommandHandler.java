package com.lms.leave.features.cancel;

import com.lms.leave.common.exceptions.LeaveAlreadyCancelledException;
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
public class CancelLeaveCommandHandler implements ICommandHandler<CancelLeaveCommand, Result<LeaveResponse>> {

    private final LeaveRequestRepository leaveRequestRepository;
    private final LeaveEventProducer leaveEventProducer;

    public CancelLeaveCommandHandler(LeaveRequestRepository leaveRequestRepository,
                                     LeaveEventProducer leaveEventProducer) {
        this.leaveRequestRepository = leaveRequestRepository;
        this.leaveEventProducer     = leaveEventProducer;
    }

    @Override
    @Transactional
    public Result<LeaveResponse> handle(CancelLeaveCommand command) {
        LeaveRequest leaveRequest = leaveRequestRepository.findById(command.getLeaveRequestId())
                .orElseThrow(() -> new LeaveRequestNotFoundException(command.getLeaveRequestId()));

        if (!leaveRequest.getEmployeeId().equals(command.getEmployeeId())) {
            throw new UnauthorizedLeaveActionException("You can only cancel your own leave requests");
        }

        if (leaveRequest.getStatus() == LeaveStatus.CANCELLED) {
            throw new LeaveAlreadyCancelledException(command.getLeaveRequestId());
        }

        if (leaveRequest.getStatus() != LeaveStatus.PENDING) {
            throw new LeaveNotPendingException(command.getLeaveRequestId());
        }

        leaveRequest.setStatus(LeaveStatus.CANCELLED);
        leaveRequest = leaveRequestRepository.save(leaveRequest);

        leaveEventProducer.publishCancelled(
                leaveRequest.getId(), leaveRequest.getEmployeeId(),
                leaveRequest.getLeaveType().name());

        return Result.success(LeaveResponse.from(leaveRequest));
    }
}
