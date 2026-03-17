package com.lms.leave.infrastructure.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class LeaveEventProducer {

    private static final Logger log = LoggerFactory.getLogger(LeaveEventProducer.class);

    private static final String TOPIC_APPLIED    = "leave.applied";
    private static final String TOPIC_APPROVED   = "leave.approved";
    private static final String TOPIC_REJECTED   = "leave.rejected";
    private static final String TOPIC_CANCELLED  = "leave.cancelled";

    private final KafkaTemplate<String, LeaveEvent> kafkaTemplate;

    public LeaveEventProducer(KafkaTemplate<String, LeaveEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishApplied(Long leaveRequestId, Long employeeId,
                                String leaveType, Integer totalDays) {
        LeaveEvent event = LeaveEvent.applied(leaveRequestId, employeeId, leaveType, totalDays);
        send(TOPIC_APPLIED, leaveRequestId, event);
    }

    public void publishApproved(Long leaveRequestId, Long employeeId,
                                 String leaveType, Integer totalDays) {
        LeaveEvent event = LeaveEvent.approved(leaveRequestId, employeeId, leaveType, totalDays);
        send(TOPIC_APPROVED, leaveRequestId, event);
    }

    public void publishRejected(Long leaveRequestId, Long employeeId,
                                 String leaveType, String rejectionReason) {
        LeaveEvent event = LeaveEvent.rejected(leaveRequestId, employeeId, leaveType, rejectionReason);
        send(TOPIC_REJECTED, leaveRequestId, event);
    }

    public void publishCancelled(Long leaveRequestId, Long employeeId, String leaveType) {
        LeaveEvent event = LeaveEvent.cancelled(leaveRequestId, employeeId, leaveType);
        send(TOPIC_CANCELLED, leaveRequestId, event);
    }

    private void send(String topic, Long key, LeaveEvent event) {
        kafkaTemplate.send(topic, String.valueOf(key), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish event to topic {}: {}", topic, ex.getMessage());
                    } else {
                        log.info("Published event to topic {} partition {} offset {}",
                                topic,
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }
}
