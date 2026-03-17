package com.lms.leave.integration;

import com.lms.leave.infrastructure.kafka.LeaveEvent;
import com.lms.leave.infrastructure.persistence.entity.LeaveRequest;
import com.lms.leave.infrastructure.persistence.entity.LeaveStatus;
import com.lms.leave.infrastructure.persistence.entity.LeaveType;
import com.lms.leave.infrastructure.persistence.repository.LeaveRequestRepository;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class LeaveKafkaEventIntegrationTest extends BaseIntegrationTest {

    @Autowired TestRestTemplate restTemplate;
    @Autowired LeaveRequestRepository leaveRequestRepository;

    @BeforeEach
    void setUp() {
        leaveRequestRepository.deleteAll();
        when(employeeServiceClient.getRemainingDays(5L, "ANNUAL", "test-token")).thenReturn(20);
    }

    @Test
    void applyLeave_shouldPublishLeaveAppliedEventToKafka() throws Exception {
        HttpHeaders headers = employeeHeaders(5L, 2L);
        Map<String, Object> body = Map.of(
                "leaveType", "ANNUAL",
                "startDate", LocalDate.now().plusDays(1).toString(),
                "endDate",   LocalDate.now().plusDays(3).toString(),
                "reason",    "Vacation"
        );

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/leaves", HttpMethod.POST,
                new HttpEntity<>(body, headers), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // Poll Kafka for the published event
        try (KafkaConsumer<String, LeaveEvent> consumer = kafkaConsumer("leave.applied")) {
            ConsumerRecords<String, LeaveEvent> records = consumer.poll(Duration.ofSeconds(10));

            assertThat(records.count()).isGreaterThanOrEqualTo(1);

            LeaveEvent event = records.iterator().next().value();
            assertThat(event.getEmployeeId()).isEqualTo(5L);
            assertThat(event.getLeaveType()).isEqualTo("ANNUAL");
            assertThat(event.getTotalDays()).isEqualTo(3);
        }
    }

    @Test
    void approveLeave_shouldPublishLeaveApprovedEventToKafka() throws Exception {
        LeaveRequest pending = leaveRequestRepository.save(pendingLeaveRequest(5L, 2L));

        restTemplate.exchange(
                "/api/leaves/" + pending.getId() + "/approve",
                HttpMethod.PUT, new HttpEntity<>(managerHeaders(2L)), Map.class);

        try (KafkaConsumer<String, LeaveEvent> consumer = kafkaConsumer("leave.approved")) {
            ConsumerRecords<String, LeaveEvent> records = consumer.poll(Duration.ofSeconds(10));

            assertThat(records.count()).isGreaterThanOrEqualTo(1);

            LeaveEvent event = records.iterator().next().value();
            assertThat(event.getEmployeeId()).isEqualTo(5L);
            assertThat(event.getLeaveType()).isEqualTo("ANNUAL");
        }
    }

    private KafkaConsumer<String, LeaveEvent> kafkaConsumer(String topic) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "test-" + System.currentTimeMillis());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, LeaveEvent.class.getName());

        KafkaConsumer<String, LeaveEvent> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(List.of(topic));
        return consumer;
    }

    private LeaveRequest pendingLeaveRequest(Long employeeId, Long managerId) {
        LeaveRequest lr = new LeaveRequest();
        lr.setEmployeeId(employeeId);
        lr.setManagerId(managerId);
        lr.setLeaveType(LeaveType.ANNUAL);
        lr.setStartDate(LocalDate.now().plusDays(1));
        lr.setEndDate(LocalDate.now().plusDays(3));
        lr.setReason("Vacation");
        lr.setStatus(LeaveStatus.PENDING);
        return lr;
    }

    private HttpHeaders employeeHeaders(Long employeeId, Long managerId) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Employee-Id", String.valueOf(employeeId));
        headers.set("X-Employee-Role", "EMPLOYEE");
        headers.set("X-Manager-Id", String.valueOf(managerId));
        headers.set("Authorization", "Bearer test-token");
        return headers;
    }

    private HttpHeaders managerHeaders(Long managerId) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Employee-Id", String.valueOf(managerId));
        headers.set("X-Employee-Role", "MANAGER");
        return headers;
    }
}
