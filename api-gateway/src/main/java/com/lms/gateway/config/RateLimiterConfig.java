package com.lms.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

@Configuration
public class RateLimiterConfig {

    /**
     * Rate-limit per employee (from the forwarded X-Employee-Id header).
     * Falls back to the remote IP for unauthenticated public routes.
     */
    @Bean
    public KeyResolver employeeKeyResolver() {
        return exchange -> {
            String employeeId = exchange.getRequest().getHeaders().getFirst("X-Employee-Id");
            if (employeeId != null) {
                return Mono.just("employee:" + employeeId);
            }
            String ip = exchange.getRequest().getRemoteAddress() != null
                    ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                    : "unknown";
            return Mono.just("ip:" + ip);
        };
    }

    @Bean
    public RedisRateLimiter redisRateLimiter() {
        // replenishRate=10 req/s, burstCapacity=20
        return new RedisRateLimiter(10, 20);
    }
}
