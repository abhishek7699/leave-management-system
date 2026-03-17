package com.lms.employee.features.employee;

import com.lms.employee.common.mediator.Mediator;
import com.lms.employee.common.result.Result;
import com.lms.employee.features.employee.delete.DeleteEmployeeCommand;
import com.lms.employee.features.employee.getById.GetEmployeeByIdQuery;
import com.lms.employee.features.employee.list.ListEmployeesQuery;
import com.lms.employee.features.employee.update.UpdateEmployeeCommand;
import com.lms.employee.features.employee.update.UpdateEmployeeRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {

    private final Mediator mediator;

    public EmployeeController(Mediator mediator) {
        this.mediator = mediator;
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmployeeResponse> getById(
            @PathVariable Long id,
            @RequestHeader("X-Employee-Id") Long requesterId,
            @RequestHeader("X-Employee-Role") String requesterRole) {

        Result<EmployeeResponse> result = mediator.query(
                new GetEmployeeByIdQuery(id, requesterId, requesterRole));
        return ResponseEntity.ok(result.getValue());
    }

    @GetMapping
    public ResponseEntity<List<EmployeeResponse>> list() {
        Result<List<EmployeeResponse>> result = mediator.query(new ListEmployeesQuery());
        return ResponseEntity.ok(result.getValue());
    }

    @PutMapping("/{id}")
    public ResponseEntity<EmployeeResponse> update(
            @PathVariable Long id,
            @RequestHeader("X-Employee-Id") Long requesterId,
            @Valid @RequestBody UpdateEmployeeRequest request) {

        Result<EmployeeResponse> result = mediator.send(
                new UpdateEmployeeCommand(id, requesterId, request.getName(), request.getDepartment()));
        return ResponseEntity.ok(result.getValue());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        mediator.send(new DeleteEmployeeCommand(id));
        return ResponseEntity.noContent().build();
    }
}
