package com.lms.leave.features.getById;

public class GetLeaveByIdQuery {

    private final Long leaveRequestId;
    private final Long requesterId;
    private final String requesterRole;
    private final Long requesterManagerId;

    public GetLeaveByIdQuery(Long leaveRequestId, Long requesterId,
                             String requesterRole, Long requesterManagerId) {
        this.leaveRequestId    = leaveRequestId;
        this.requesterId       = requesterId;
        this.requesterRole     = requesterRole;
        this.requesterManagerId = requesterManagerId;
    }

    public Long getLeaveRequestId()     { return leaveRequestId; }
    public Long getRequesterId()        { return requesterId; }
    public String getRequesterRole()    { return requesterRole; }
    public Long getRequesterManagerId() { return requesterManagerId; }
}
