package com.lms.leave.features.apply;

import com.lms.leave.common.exceptions.OverlappingLeaveException;
import com.lms.leave.common.mediator.ICommandHandler;
import com.lms.leave.common.result.Result;
import com.lms.leave.features.LeaveResponse;
import com.lms.leave.infrastructure.client.EmployeeServiceClient;
import com.lms.leave.infrastructure.kafka.LeaveEventProducer;
import com.lms.leave.infrastructure.persistence.entity.LeaveRequest;
import com.lms.leave.infrastructure.persistence.entity.LeaveType;
import com.lms.leave.infrastructure.persistence.repository.LeaveRequestRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Component
public class ApplyLeaveCommandHandler implements ICommandHandler<ApplyLeaveCommand, Result<LeaveResponse>> {

    private final LeaveRequestRepository leaveRequestRepository;
    private final EmployeeServiceClient employeeServiceClient;
    private final LeaveEventProducer leaveEventProducer;

    public ApplyLeaveCommandHandler(LeaveRequestRepository leaveRequestRepository,
                                    EmployeeServiceClient employeeServiceClient,
                                    LeaveEventProducer leaveEventProducer) {
        this.leaveRequestRepository = leaveRequestRepository;
        this.employeeServiceClient  = employeeServiceClient;
        this.leaveEventProducer     = leaveEventProducer;
    }

    @Override
    @Transactional
    public Result<LeaveResponse> handle(ApplyLeaveCommand command) {
        if (!command.getEndDate().isAfter(command.getStartDate())
                && !command.getEndDate().isEqual(command.getStartDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "End date must be >= start date");
        }

        int totalDays = (int) (command.getEndDate().toEpochDay() - command.getStartDate().toEpochDay()) + 1;

        // Check balance (skip for UNPAID)
        if (command.getLeaveType() != LeaveType.UNPAID) {
            int remaining = employeeServiceClient.getRemainingDays(
                    command.getEmployeeId(), command.getLeaveType().name(), command.getBearerToken());

            if (remaining < totalDays) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Insufficient " + command.getLeaveType() + " leave balance. " +
                        "Requested: " + totalDays + ", Remaining: " + remaining);
            }
        }

        // Check overlapping leaves
        if (!leaveRequestRepository.findOverlapping(
                command.getEmployeeId(), command.getLeaveType(),
                command.getStartDate(), command.getEndDate()).isEmpty()) {
            throw new OverlappingLeaveException();
        }

        LeaveRequest leaveRequest = new LeaveRequest();
        leaveRequest.setEmployeeId(command.getEmployeeId());
        leaveRequest.setManagerId(command.getManagerId());
        leaveRequest.setLeaveType(command.getLeaveType());
        leaveRequest.setStartDate(command.getStartDate());
        leaveRequest.setEndDate(command.getEndDate());
        leaveRequest.setReason(command.getReason());

        leaveRequest = leaveRequestRepository.save(leaveRequest);

        leaveEventProducer.publishApplied(
                leaveRequest.getId(), leaveRequest.getEmployeeId(),
                leaveRequest.getLeaveType().name(), leaveRequest.getTotalDays());

        return Result.success(LeaveResponse.from(leaveRequest));
    }
}
