package com.lms.gateway.filter;

import com.lms.gateway.security.JwtBlacklistService;
import com.lms.gateway.security.JwtService;
import io.jsonwebtoken.Claims;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class AuthFilter extends AbstractGatewayFilterFactory<AuthFilter.Config> {

    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/employees/auth/register",
            "/api/employees/auth/login"
    );

    private final JwtService jwtService;
    private final JwtBlacklistService blacklistService;

    public AuthFilter(JwtService jwtService, JwtBlacklistService blacklistService) {
        super(Config.class);
        this.jwtService       = jwtService;
        this.blacklistService = blacklistService;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String path = exchange.getRequest().getURI().getPath();

            if (isPublic(path)) {
                return chain.filter(exchange);
            }

            String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return unauthorized(exchange);
            }

            String token = authHeader.substring(7);

            if (!jwtService.isTokenValid(token)) {
                return unauthorized(exchange);
            }

            return blacklistService.isBlacklisted(token)
                    .flatMap(blacklisted -> {
                        if (Boolean.TRUE.equals(blacklisted)) {
                            return unauthorized(exchange);
                        }

                        Claims claims = jwtService.extractAllClaims(token);

                        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                                .header("X-Employee-Id",     String.valueOf(claims.get("employeeId")))
                                .header("X-Employee-Role",   String.valueOf(claims.get("role")))
                                .header("X-Employee-Department", String.valueOf(claims.get("department")))
                                .header("X-Manager-Id",      managerIdHeader(claims))
                                .build();

                        return chain.filter(exchange.mutate().request(mutatedRequest).build());
                    });
        };
    }

    private boolean isPublic(String path) {
        return PUBLIC_PATHS.stream().anyMatch(path::equals);
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }

    private String managerIdHeader(Claims claims) {
        Object managerId = claims.get("managerId");
        return managerId == null ? "" : String.valueOf(managerId);
    }

    public static class Config {}
}
