package com.lms.employee.infrastructure.redis;

import com.lms.employee.infrastructure.persistence.entity.LeaveType;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
public class LeaveBalanceCache {

    private static final String PREFIX = "balance:";
    private static final long TTL_SECONDS = 3600;

    private final RedisTemplate<String, String> redisTemplate;

    public LeaveBalanceCache(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void put(Long employeeId, LeaveType leaveType, int remainingDays) {
        redisTemplate.opsForValue()
                .set(key(employeeId, leaveType), String.valueOf(remainingDays), TTL_SECONDS, TimeUnit.SECONDS);
    }

    public Optional<Integer> get(Long employeeId, LeaveType leaveType) {
        String value = redisTemplate.opsForValue().get(key(employeeId, leaveType));
        return value == null ? Optional.empty() : Optional.of(Integer.parseInt(value));
    }

    public void evict(Long employeeId, LeaveType leaveType) {
        redisTemplate.delete(key(employeeId, leaveType));
    }

    public void evictAll(Long employeeId) {
        for (LeaveType type : LeaveType.values()) {
            evict(employeeId, type);
        }
    }

    private String key(Long employeeId, LeaveType leaveType) {
        return PREFIX + employeeId + ":" + leaveType.name();
    }
}
