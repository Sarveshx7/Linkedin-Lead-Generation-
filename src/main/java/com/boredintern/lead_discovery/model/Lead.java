package com.boredintern.lead_discovery.model;

import jakarta.persistence.*;
import org.hibernate.annotations.Formula;
import java.time.Instant;

@Entity
@Table(name = "leads", indexes = {
    @Index(name = "idx_lead_fingerprint", columnList = "fingerprint", unique = true),
    @Index(name = "idx_lead_discovered_at", columnList = "discoveredAt"),
    @Index(name = "idx_lead_review_status", columnList = "reviewStatus"),
    @Index(name = "idx_lead_service_cat", columnList = "serviceCategory"),
    @Index(name = "idx_lead_intent_lvl", columnList = "intentLevel"),
    @Index(name = "idx_lead_indian_market", columnList = "isIndianMarket")
})
public class Lead {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String fingerprint;

    @Column(length = 1000)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(columnDefinition = "TEXT")
    private String sourceUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private SourcePlatform sourcePlatform;

    private String author;

    @Column(columnDefinition = "TEXT")
    private String authorProfileUrl;

    private String company;

    private String location;

    private Instant publishedAt;

    @Column(nullable = false)
    private Instant discoveredAt = Instant.now();

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private ServiceCategory serviceCategory = ServiceCategory.OTHER;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private IntentLevel intentLevel = IntentLevel.MEDIUM;

    @Formula("CASE WHEN intent_level = 'HIGH' THEN 3 WHEN intent_level = 'MEDIUM' THEN 2 ELSE 1 END")
    private Integer intentPriority;

    @Column(columnDefinition = "TEXT")
    private String intentReasoning;

    private Integer relevanceScore = 50;

    private Boolean isIndianMarket = false;

    @Column(columnDefinition = "TEXT")
    private String detectedKeywords;

    @Column(columnDefinition = "TEXT")
    private String contactInfo;

    @Column(length = 255)
    private String searchProviders;

    private Integer providerMatchCount = 0;

    private Integer providerCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReviewStatus reviewStatus = ReviewStatus.DISCOVERED;

    private Integer completenessScore = 50;

    private Long campaignId;

    @Column(columnDefinition = "TEXT")
    private String userNotes;

    public Lead() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFingerprint() {
        return fingerprint;
    }

    public void setFingerprint(String fingerprint) {
        this.fingerprint = fingerprint;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public SourcePlatform getSourcePlatform() {
        return sourcePlatform;
    }

    public void setSourcePlatform(SourcePlatform sourcePlatform) {
        this.sourcePlatform = sourcePlatform;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getAuthorProfileUrl() {
        return authorProfileUrl;
    }

    public void setAuthorProfileUrl(String authorProfileUrl) {
        this.authorProfileUrl = authorProfileUrl;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(Instant publishedAt) {
        this.publishedAt = publishedAt;
    }

    public Instant getDiscoveredAt() {
        return discoveredAt;
    }

    public void setDiscoveredAt(Instant discoveredAt) {
        this.discoveredAt = discoveredAt;
    }

    public ServiceCategory getServiceCategory() {
        return serviceCategory;
    }

    public void setServiceCategory(ServiceCategory serviceCategory) {
        this.serviceCategory = serviceCategory;
    }

    public IntentLevel getIntentLevel() {
        return intentLevel;
    }

    public void setIntentLevel(IntentLevel intentLevel) {
        this.intentLevel = intentLevel;
    }

    @com.fasterxml.jackson.annotation.JsonIgnore
    public Integer getIntentPriority() {
        return intentPriority;
    }

    public String getIntentReasoning() {
        return intentReasoning;
    }

    public void setIntentReasoning(String intentReasoning) {
        this.intentReasoning = intentReasoning;
    }

    public Integer getRelevanceScore() {
        return relevanceScore;
    }

    public void setRelevanceScore(Integer relevanceScore) {
        this.relevanceScore = relevanceScore;
    }

    public Boolean getIsIndianMarket() {
        return isIndianMarket;
    }

    public void setIsIndianMarket(Boolean isIndianMarket) {
        this.isIndianMarket = isIndianMarket;
    }

    public String getDetectedKeywords() {
        return detectedKeywords;
    }

    public void setDetectedKeywords(String detectedKeywords) {
        this.detectedKeywords = detectedKeywords;
    }

    public String getContactInfo() {
        return contactInfo;
    }

    public void setContactInfo(String contactInfo) {
        this.contactInfo = contactInfo;
    }

    public String getSearchProviders() {
        return searchProviders;
    }

    public void setSearchProviders(String searchProviders) {
        this.searchProviders = searchProviders;
    }

    public Integer getProviderMatchCount() {
        return providerMatchCount;
    }

    public void setProviderMatchCount(Integer providerMatchCount) {
        this.providerMatchCount = providerMatchCount;
    }

    public Integer getProviderCount() {
        return providerCount;
    }

    public void setProviderCount(Integer providerCount) {
        this.providerCount = providerCount;
    }

    public ReviewStatus getReviewStatus() {
        return reviewStatus;
    }

    public void setReviewStatus(ReviewStatus reviewStatus) {
        this.reviewStatus = reviewStatus;
    }

    public Integer getCompletenessScore() {
        return completenessScore;
    }

    public void setCompletenessScore(Integer completenessScore) {
        this.completenessScore = completenessScore;
    }

    public Long getCampaignId() {
        return campaignId;
    }

    public void setCampaignId(Long campaignId) {
        this.campaignId = campaignId;
    }

    public String getUserNotes() {
        return userNotes;
    }

    public void setUserNotes(String userNotes) {
        this.userNotes = userNotes;
    }
}
