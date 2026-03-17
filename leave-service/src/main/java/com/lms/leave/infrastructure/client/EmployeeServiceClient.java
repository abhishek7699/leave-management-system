package com.lms.leave.infrastructure.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Component
public class EmployeeServiceClient {

    private final RestTemplate restTemplate;
    private final String employeeServiceUrl;

    public EmployeeServiceClient(
            RestTemplateBuilder builder,
            @Value("${employee-service.url}") String employeeServiceUrl) {
        this.restTemplate = builder.build();
        this.employeeServiceUrl = employeeServiceUrl;
    }

    /**
     * Fetches remaining leave days for the given employee and leave type.
     * Calls GET /api/employees/{employeeId}/balance/{leaveType}
     * Returns -1 on any failure (caller treats this as insufficient balance).
     *
     * The employee-service balance endpoint requires X-Employee-Id and X-Employee-Role
     * headers (normally injected by the API Gateway). For service-to-service calls
     * that bypass the gateway, we supply them directly using the known employeeId
     * and role EMPLOYEE (employees always check their own balance on apply).
     */
    @SuppressWarnings("unchecked")
    public int getRemainingDays(Long employeeId, String leaveType, String bearerToken) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken);
            headers.set("X-Employee-Id", String.valueOf(employeeId));
            headers.set("X-Employee-Role", "EMPLOYEE");

            String url = employeeServiceUrl + "/api/employees/" + employeeId + "/balance/" + leaveType;
            ResponseEntity<List> response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers), List.class);

            if (response.getBody() != null && !response.getBody().isEmpty()) {
                Map<String, Object> balance = (Map<String, Object>) response.getBody().get(0);
                Object remaining = balance.get("remainingDays");
                if (remaining instanceof Integer) return (Integer) remaining;
                if (remaining instanceof Number) return ((Number) remaining).intValue();
            }
        } catch (Exception ignored) {}
        return -1;
    }
}
