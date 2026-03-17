package com.lms.employee.features.auth.logout;

import com.lms.employee.common.mediator.ICommandHandler;
import com.lms.employee.common.result.Result;
import com.lms.employee.common.security.JwtService;
import com.lms.employee.infrastructure.redis.JwtBlacklistService;
import org.springframework.stereotype.Component;

@Component
public class LogoutCommandHandler implements ICommandHandler<LogoutCommand, Result<Void>> {

    private final JwtBlacklistService blacklistService;
    private final JwtService jwtService;

    public LogoutCommandHandler(JwtBlacklistService blacklistService, JwtService jwtService) {
        this.blacklistService = blacklistService;
        this.jwtService = jwtService;
    }

    @Override
    public Result<Void> handle(LogoutCommand command) {
        long ttlMs = jwtService.getRemainingTtlMs(command.getToken());
        blacklistService.blacklist(command.getToken(), ttlMs);
        return Result.success();
    }
}
