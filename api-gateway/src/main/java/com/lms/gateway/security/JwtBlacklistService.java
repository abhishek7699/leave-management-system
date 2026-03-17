package com.lms.gateway.security;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class JwtBlacklistService {

    private static final String PREFIX = "blacklist:";

    private final ReactiveRedisTemplate<String, String> redisTemplate;

    public JwtBlacklistService(@Qualifier("reactiveStringRedisTemplate") ReactiveRedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public Mono<Boolean> isBlacklisted(String token) {
        return redisTemplate.hasKey(PREFIX + token);
    }
}
