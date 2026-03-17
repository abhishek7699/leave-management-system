package com.lms.employee.features.auth.logout;

import com.lms.employee.common.result.Result;
import com.lms.employee.common.security.JwtService;
import com.lms.employee.infrastructure.redis.JwtBlacklistService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LogoutCommandHandlerTest {

    @Mock JwtBlacklistService blacklistService;
    @Mock JwtService jwtService;

    @InjectMocks LogoutCommandHandler handler;

    @Test
    void handle_whenTokenIsValid_shouldBlacklistWithRemainingTtl() {
        String token = "valid.jwt.token";
        when(jwtService.getRemainingTtlMs(token)).thenReturn(3600_000L);

        Result<Void> result = handler.handle(new LogoutCommand(token));

        assertThat(result.isSuccess()).isTrue();
        verify(blacklistService).blacklist(token, 3600_000L);
    }

    @Test
    void handle_whenTokenIsAlreadyExpired_shouldBlacklistWithZeroTtl() {
        String token = "expired.jwt.token";
        when(jwtService.getRemainingTtlMs(token)).thenReturn(0L);

        Result<Void> result = handler.handle(new LogoutCommand(token));

        assertThat(result.isSuccess()).isTrue();
        verify(blacklistService).blacklist(token, 0L);
    }
}
