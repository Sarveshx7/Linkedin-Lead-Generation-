package com.boredintern.lead_discovery.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "collection_runs", indexes = {
    @Index(name = "idx_run_start_time", columnList = "startTime"),
    @Index(name = "idx_run_status", columnList = "status")
})
public class CollectionRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long campaignId;

    private String campaignName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RunStatus status = RunStatus.PENDING;

    @Column(nullable = false)
    private Instant startTime = Instant.now();

    private Instant endTime;

    private Integer leadsDiscovered = 0;

    private Integer duplicatesSkipped = 0;

    private Integer sourcesAttempted = 0;

    @Column(columnDefinition = "TEXT")
    private String errorLogs;

    public CollectionRun() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCampaignId() {
        return campaignId;
    }

    public void setCampaignId(Long campaignId) {
        this.campaignId = campaignId;
    }

    public String getCampaignName() {
        return campaignName;
    }

    public void setCampaignName(String campaignName) {
        this.campaignName = campaignName;
    }

    public RunStatus getStatus() {
        return status;
    }

    public void setStatus(RunStatus status) {
        this.status = status;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public void setStartTime(Instant startTime) {
        this.startTime = startTime;
    }

    public Instant getEndTime() {
        return endTime;
    }

    public void setEndTime(Instant endTime) {
        this.endTime = endTime;
    }

    public Integer getLeadsDiscovered() {
        return leadsDiscovered;
    }

    public void setLeadsDiscovered(Integer leadsDiscovered) {
        this.leadsDiscovered = leadsDiscovered;
    }

    public Integer getDuplicatesSkipped() {
        return duplicatesSkipped;
    }

    public void setDuplicatesSkipped(Integer duplicatesSkipped) {
        this.duplicatesSkipped = duplicatesSkipped;
    }

    public Integer getSourcesAttempted() {
        return sourcesAttempted;
    }

    public void setSourcesAttempted(Integer sourcesAttempted) {
        this.sourcesAttempted = sourcesAttempted;
    }

    public String getErrorLogs() {
        return errorLogs;
    }

    public void setErrorLogs(String errorLogs) {
        this.errorLogs = errorLogs;
    }
}
