package com.lms.employee.features.auth.register;

import com.lms.employee.common.mediator.Mediator;
import com.lms.employee.common.result.Result;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/employees/auth")
public class RegisterController {

    private final Mediator mediator;

    public RegisterController(Mediator mediator) {
        this.mediator = mediator;
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@Valid @RequestBody RegisterRequest request) {
        RegisterCommand command = new RegisterCommand(
                request.getName(),
                request.getEmail(),
                request.getPassword(),
                request.getRole(),
                request.getDepartment(),
                request.getManagerId()
        );

        Result<Long> result = mediator.send(command);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("employeeId", result.getValue()));
    }
}
