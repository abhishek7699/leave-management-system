package com.lms.employee.features.employee.getById;

public class GetEmployeeByIdQuery {

    private final Long id;
    private final Long requesterId;
    private final String requesterRole;

    public GetEmployeeByIdQuery(Long id, Long requesterId, String requesterRole) {
        this.id = id;
        this.requesterId = requesterId;
        this.requesterRole = requesterRole;
    }

    public Long getId() { return id; }
    public Long getRequesterId() { return requesterId; }
    public String getRequesterRole() { return requesterRole; }
}
