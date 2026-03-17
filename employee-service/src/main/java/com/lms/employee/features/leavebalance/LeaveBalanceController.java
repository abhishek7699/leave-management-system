package com.lms.employee.features.leavebalance;

import com.lms.employee.common.mediator.Mediator;
import com.lms.employee.common.result.Result;
import com.lms.employee.features.leavebalance.getBalance.GetLeaveBalanceQuery;
import com.lms.employee.infrastructure.persistence.entity.LeaveType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/employees/{id}/balance")
public class LeaveBalanceController {

    private final Mediator mediator;

    public LeaveBalanceController(Mediator mediator) {
        this.mediator = mediator;
    }

    @GetMapping
    public ResponseEntity<List<LeaveBalanceResponse>> getAll(
            @PathVariable Long id,
            @RequestHeader("X-Employee-Id") Long requesterId,
            @RequestHeader("X-Employee-Role") String requesterRole) {

        Result<List<LeaveBalanceResponse>> result = mediator.query(
                new GetLeaveBalanceQuery(id, null, requesterId, requesterRole));
        return ResponseEntity.ok(result.getValue());
    }

    @GetMapping("/{leaveType}")
    public ResponseEntity<List<LeaveBalanceResponse>> getByType(
            @PathVariable Long id,
            @PathVariable LeaveType leaveType,
            @RequestHeader("X-Employee-Id") Long requesterId,
            @RequestHeader("X-Employee-Role") String requesterRole) {

        Result<List<LeaveBalanceResponse>> result = mediator.query(
                new GetLeaveBalanceQuery(id, leaveType, requesterId, requesterRole));
        return ResponseEntity.ok(result.getValue());
    }
}
