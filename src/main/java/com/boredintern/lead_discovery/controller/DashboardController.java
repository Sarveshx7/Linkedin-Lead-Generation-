package com.boredintern.lead_discovery.controller;

import com.boredintern.lead_discovery.dto.DashboardStatsDto;
import com.boredintern.lead_discovery.model.CollectionRun;
import com.boredintern.lead_discovery.model.IntentLevel;
import com.boredintern.lead_discovery.model.ReviewStatus;
import com.boredintern.lead_discovery.model.SourcePlatform;
import com.boredintern.lead_discovery.repository.CampaignRepository;
import com.boredintern.lead_discovery.repository.CollectionRunRepository;
import com.boredintern.lead_discovery.repository.LeadRepository;
import com.boredintern.lead_discovery.repository.LeadSpecification;
import com.boredintern.lead_discovery.dto.LeadFilterRequest;
import com.boredintern.lead_discovery.model.Lead;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "*")
public class DashboardController {

    private final LeadRepository leadRepository;
    private final CampaignRepository campaignRepository;
    private final CollectionRunRepository collectionRunRepository;

    public DashboardController(
            LeadRepository leadRepository,
            CampaignRepository campaignRepository,
            CollectionRunRepository collectionRunRepository
    ) {
        this.leadRepository = leadRepository;
        this.campaignRepository = campaignRepository;
        this.collectionRunRepository = collectionRunRepository;
    }

    @GetMapping("/stats")
    public ResponseEntity<DashboardStatsDto> getStats() {
        LeadFilterRequest freshFilter = new LeadFilterRequest();
        freshFilter.setIsIndian(true);
        List<Lead> freshLeads = leadRepository.findAll(
                LeadSpecification.withFilters(freshFilter), Sort.by(Sort.Direction.DESC, "publishedAt"));
        long totalLeads = freshLeads.size();
        long highIntentLeads = freshLeads.stream().filter(lead -> lead.getIntentLevel() == IntentLevel.HIGH).count();
        long indianMarketLeads = freshLeads.stream().filter(lead -> Boolean.TRUE.equals(lead.getIsIndianMarket())).count();
        long shortlistedLeads = freshLeads.stream().filter(lead -> lead.getReviewStatus() == ReviewStatus.SHORTLISTED).count();
        long activeCampaigns = campaignRepository.findByIsActiveTrue().size();
        long totalRuns = collectionRunRepository.count();

        Map<String, Long> categoryCounts = new LinkedHashMap<>();
        freshLeads.stream().map(Lead::getServiceCategory).filter(java.util.Objects::nonNull)
                .forEach(category -> categoryCounts.merge(category.name(), 1L, Long::sum));

        Map<String, Long> intentCounts = new LinkedHashMap<>();
        freshLeads.stream().map(Lead::getIntentLevel).filter(java.util.Objects::nonNull)
                .forEach(intent -> intentCounts.merge(intent.name(), 1L, Long::sum));

        Map<String, Long> platformCounts = new LinkedHashMap<>();
        freshLeads.stream().map(Lead::getSourcePlatform).filter(java.util.Objects::nonNull)
                .forEach(platform -> platformCounts.merge(platform.name(), 1L, Long::sum));

        List<CollectionRun> recentRuns = collectionRunRepository.findTop50ByOrderByStartTimeDesc();
        List<Lead> recentLeads = freshLeads.stream()
                .filter(lead -> lead.getIntentLevel() == IntentLevel.HIGH && lead.getSourcePlatform() == SourcePlatform.LINKEDIN)
                .limit(25).toList();

        return ResponseEntity.ok(new DashboardStatsDto(
                totalLeads,
                highIntentLeads,
                indianMarketLeads,
                shortlistedLeads,
                activeCampaigns,
                totalRuns,
                categoryCounts,
                intentCounts,
                platformCounts,
                recentRuns,
                recentLeads
        ));
    }
}
