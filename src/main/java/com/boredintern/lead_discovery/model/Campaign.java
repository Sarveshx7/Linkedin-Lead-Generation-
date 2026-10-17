package com.boredintern.lead_discovery.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "campaigns")
public class Campaign {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String keywords;

    @Column(columnDefinition = "TEXT")
    private String excludedKeywords;

    @Column(nullable = false)
    private Boolean targetIndianMarket = true;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String enabledSources = "LINKEDIN";

    @Column(columnDefinition = "TEXT")
    private String customRssFeeds;

    @Column(nullable = false)
    private Boolean isActive = true;

    private Instant lastRunAt;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    public Campaign() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getKeywords() {
        return keywords;
    }

    public void setKeywords(String keywords) {
        this.keywords = keywords;
    }

    public String getExcludedKeywords() {
        return excludedKeywords;
    }

    public void setExcludedKeywords(String excludedKeywords) {
        this.excludedKeywords = excludedKeywords;
    }

    public Boolean getTargetIndianMarket() {
        return targetIndianMarket;
    }

    public void setTargetIndianMarket(Boolean targetIndianMarket) {
        this.targetIndianMarket = targetIndianMarket;
    }

    public String getEnabledSources() {
        return enabledSources;
    }

    public void setEnabledSources(String enabledSources) {
        this.enabledSources = enabledSources;
    }

    public String getCustomRssFeeds() {
        return customRssFeeds;
    }

    public void setCustomRssFeeds(String customRssFeeds) {
        this.customRssFeeds = customRssFeeds;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public Instant getLastRunAt() {
        return lastRunAt;
    }

    public void setLastRunAt(Instant lastRunAt) {
        this.lastRunAt = lastRunAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
