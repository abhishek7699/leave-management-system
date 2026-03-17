package com.lms.employee.features.auth.login;

import com.lms.employee.common.mediator.Mediator;
import com.lms.employee.common.result.Result;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/employees/auth")
public class LoginController {

    private final Mediator mediator;

    public LoginController(Mediator mediator) {
        this.mediator = mediator;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        Result<LoginResponse> result = mediator.send(new LoginCommand(request.getEmail(), request.getPassword()));
        return ResponseEntity.ok(result.getValue());
    }
}
