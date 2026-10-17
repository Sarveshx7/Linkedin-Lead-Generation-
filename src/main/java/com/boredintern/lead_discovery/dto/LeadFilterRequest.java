package com.boredintern.lead_discovery.dto;

import com.boredintern.lead_discovery.model.IntentLevel;
import com.boredintern.lead_discovery.model.ReviewStatus;
import com.boredintern.lead_discovery.model.ServiceCategory;
import com.boredintern.lead_discovery.model.SourcePlatform;

public class LeadFilterRequest {

    private String query;
    private ServiceCategory category;
    private IntentLevel intent;
    private ReviewStatus status;
    private Boolean isIndian;
    private SourcePlatform platform;
    private int page = 0;
    private int size = 20;
    private String sortBy = "discoveredAt";
    private String sortDir = "desc";
    private String timeRange; // "24h", "week", "month", "all"

    public LeadFilterRequest() {
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public ServiceCategory getCategory() {
        return category;
    }

    public void setCategory(ServiceCategory category) {
        this.category = category;
    }

    public IntentLevel getIntent() {
        return intent;
    }

    public void setIntent(IntentLevel intent) {
        this.intent = intent;
    }

    public ReviewStatus getStatus() {
        return status;
    }

    public void setStatus(ReviewStatus status) {
        this.status = status;
    }

    public Boolean getIsIndian() {
        return isIndian;
    }

    public void setIsIndian(Boolean isIndian) {
        this.isIndian = isIndian;
    }

    public SourcePlatform getPlatform() {
        return platform;
    }

    public void setPlatform(SourcePlatform platform) {
        this.platform = platform;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public String getSortBy() {
        return sortBy;
    }

    public void setSortBy(String sortBy) {
        this.sortBy = sortBy;
    }

    public String getSortDir() {
        return sortDir;
    }

    public void setSortDir(String sortDir) {
        this.sortDir = sortDir;
    }

    public String getTimeRange() {
        return timeRange;
    }

    public void setTimeRange(String timeRange) {
        this.timeRange = timeRange;
    }
}
