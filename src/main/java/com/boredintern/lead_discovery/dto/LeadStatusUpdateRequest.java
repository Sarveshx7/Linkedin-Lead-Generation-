package com.boredintern.lead_discovery.dto;

import com.boredintern.lead_discovery.model.ReviewStatus;

public class LeadStatusUpdateRequest {

    private ReviewStatus status;
    private String userNotes;

    public LeadStatusUpdateRequest() {
    }

    public ReviewStatus getStatus() {
        return status;
    }

    public void setStatus(ReviewStatus status) {
        this.status = status;
    }

    public String getUserNotes() {
        return userNotes;
    }

    public void setUserNotes(String userNotes) {
        this.userNotes = userNotes;
    }
}
