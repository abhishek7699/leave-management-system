package com.lms.leave.features;

import com.lms.leave.common.mediator.Mediator;
import com.lms.leave.common.result.Result;
import com.lms.leave.features.approve.ApproveLeaveCommand;
import com.lms.leave.features.apply.ApplyLeaveCommand;
import com.lms.leave.features.apply.ApplyLeaveRequest;
import com.lms.leave.features.cancel.CancelLeaveCommand;
import com.lms.leave.features.getById.GetLeaveByIdQuery;
import com.lms.leave.features.list.ListLeavesByEmployeeQuery;
import com.lms.leave.features.list.ListPendingLeavesByManagerQuery;
import com.lms.leave.features.reject.RejectLeaveCommand;
import com.lms.leave.features.reject.RejectLeaveRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/leaves")
public class LeaveController {

    private final Mediator mediator;

    public LeaveController(Mediator mediator) {
        this.mediator = mediator;
    }

    @PostMapping
    public ResponseEntity<LeaveResponse> apply(
            @Valid @RequestBody ApplyLeaveRequest request,
            @RequestHeader("X-Employee-Id") Long employeeId,
            @RequestHeader(value = "X-Manager-Id", required = false) Long managerId,
            @RequestHeader("Authorization") String authHeader) {

        String token = authHeader.startsWith("Bearer ") ? authHeader.substring(7) : authHeader;

        Result<LeaveResponse> result = mediator.send(new ApplyLeaveCommand(
                employeeId, managerId,
                request.getLeaveType(), request.getStartDate(), request.getEndDate(),
                request.getReason(), token));

        return ResponseEntity.status(HttpStatus.CREATED).body(result.getValue());
    }

    @GetMapping("/{id}")
    public ResponseEntity<LeaveResponse> getById(
            @PathVariable Long id,
            @RequestHeader("X-Employee-Id") Long requesterId,
            @RequestHeader("X-Employee-Role") String requesterRole,
            @RequestHeader(value = "X-Manager-Id", required = false) Long requesterManagerId) {

        Result<LeaveResponse> result = mediator.query(
                new GetLeaveByIdQuery(id, requesterId, requesterRole, requesterManagerId));
        return ResponseEntity.ok(result.getValue());
    }

    @GetMapping("/my")
    public ResponseEntity<List<LeaveResponse>> myLeaves(
            @RequestHeader("X-Employee-Id") Long employeeId) {

        Result<List<LeaveResponse>> result = mediator.query(
                new ListLeavesByEmployeeQuery(employeeId));
        return ResponseEntity.ok(result.getValue());
    }

    @GetMapping("/pending")
    public ResponseEntity<List<LeaveResponse>> pendingLeaves(
            @RequestHeader("X-Employee-Id") Long managerId) {

        Result<List<LeaveResponse>> result = mediator.query(
                new ListPendingLeavesByManagerQuery(managerId));
        return ResponseEntity.ok(result.getValue());
    }

    @PutMapping("/{id}/approve")
    public ResponseEntity<LeaveResponse> approve(
            @PathVariable Long id,
            @RequestHeader("X-Employee-Id") Long managerId) {

        Result<LeaveResponse> result = mediator.send(new ApproveLeaveCommand(id, managerId));
        return ResponseEntity.ok(result.getValue());
    }

    @PutMapping("/{id}/reject")
    public ResponseEntity<LeaveResponse> reject(
            @PathVariable Long id,
            @RequestHeader("X-Employee-Id") Long managerId,
            @Valid @RequestBody RejectLeaveRequest request) {

        Result<LeaveResponse> result = mediator.send(
                new RejectLeaveCommand(id, managerId, request.getRejectionReason()));
        return ResponseEntity.ok(result.getValue());
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<LeaveResponse> cancel(
            @PathVariable Long id,
            @RequestHeader("X-Employee-Id") Long employeeId) {

        Result<LeaveResponse> result = mediator.send(new CancelLeaveCommand(id, employeeId));
        return ResponseEntity.ok(result.getValue());
    }
}
