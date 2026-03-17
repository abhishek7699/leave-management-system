package com.lms.employee.infrastructure.kafka;

import com.lms.employee.infrastructure.persistence.entity.LeaveType;
import com.lms.employee.infrastructure.persistence.repository.LeaveBalanceRepository;
import com.lms.employee.infrastructure.redis.LeaveBalanceCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class LeaveEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(LeaveEventConsumer.class);

    private final LeaveBalanceRepository leaveBalanceRepository;
    private final LeaveBalanceCache leaveBalanceCache;

    public LeaveEventConsumer(LeaveBalanceRepository leaveBalanceRepository,
                              LeaveBalanceCache leaveBalanceCache) {
        this.leaveBalanceRepository = leaveBalanceRepository;
        this.leaveBalanceCache = leaveBalanceCache;
    }

    @KafkaListener(topics = "leave.approved", groupId = "employee-service")
    public void onLeaveApproved(LeaveApprovedEvent event) {
        LeaveType leaveType = LeaveType.valueOf(event.getLeaveType());

        leaveBalanceRepository
                .findByEmployeeIdAndLeaveType(event.getEmployeeId(), leaveType)
                .ifPresentOrElse(balance -> {
                    balance.setUsedDays(balance.getUsedDays() + event.getTotalDays());
                    leaveBalanceRepository.save(balance);
                    leaveBalanceCache.evict(event.getEmployeeId(), leaveType);
                    log.info("Deducted {} {} days for employee {}",
                            event.getTotalDays(), leaveType, event.getEmployeeId());
                }, () -> log.warn("LeaveBalance not found for employee {} type {}",
                        event.getEmployeeId(), leaveType));
    }

    @KafkaListener(topics = "leave.rejected", groupId = "employee-service")
    public void onLeaveRejected(LeaveApprovedEvent event) {
        log.info("Leave rejected for employee {} — no balance change", event.getEmployeeId());
    }

    @KafkaListener(topics = "leave.cancelled", groupId = "employee-service")
    public void onLeaveCancelled(LeaveApprovedEvent event) {
        log.info("Leave cancelled for employee {} — no balance change", event.getEmployeeId());
    }
}
