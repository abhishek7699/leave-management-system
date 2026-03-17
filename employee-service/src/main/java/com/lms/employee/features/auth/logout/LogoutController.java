package com.lms.employee.features.auth.logout;

import com.lms.employee.common.mediator.Mediator;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/employees/auth")
public class LogoutController {

    private final Mediator mediator;

    public LogoutController(Mediator mediator) {
        this.mediator = mediator;
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        mediator.send(new LogoutCommand(token));
        return ResponseEntity.noContent().build();
    }
}
