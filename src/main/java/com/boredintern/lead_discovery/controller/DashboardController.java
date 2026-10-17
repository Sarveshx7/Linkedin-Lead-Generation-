package com.boredintern.lead_discovery.controller;

import com.boredintern.lead_discovery.dto.DashboardStatsDto;
import com.boredintern.lead_discovery.model.CollectionRun;
import com.boredintern.lead_discovery.model.IntentLevel;
import com.boredintern.lead_discovery.model.ReviewStatus;
import com.boredintern.lead_discovery.model.SourcePlatform;
import com.boredintern.lead_discovery.repository.CampaignRepository;
import com.boredintern.lead_discovery.repository.CollectionRunRepository;
import com.boredintern.lead_discovery.repository.LeadRepository;
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
        long totalLeads = leadRepository.count();
        long highIntentLeads = leadRepository.countByIntentLevel(IntentLevel.HIGH);
        long indianMarketLeads = leadRepository.countByIsIndianMarket(true);
        long shortlistedLeads = leadRepository.countByReviewStatus(ReviewStatus.SHORTLISTED);
        long activeCampaigns = campaignRepository.findByIsActiveTrue().size();
        long totalRuns = collectionRunRepository.count();

        Map<String, Long> categoryCounts = new LinkedHashMap<>();
        for (Object[] row : leadRepository.countGroupByServiceCategory()) {
            if (row[0] != null) {
                categoryCounts.put(row[0].toString(), (Long) row[1]);
            }
        }

        Map<String, Long> intentCounts = new LinkedHashMap<>();
        for (Object[] row : leadRepository.countGroupByIntentLevel()) {
            if (row[0] != null) {
                intentCounts.put(row[0].toString(), (Long) row[1]);
            }
        }

        Map<String, Long> platformCounts = new LinkedHashMap<>();
        for (Object[] row : leadRepository.countGroupBySourcePlatform()) {
            if (row[0] != null) {
                platformCounts.put(row[0].toString(), (Long) row[1]);
            }
        }

        List<CollectionRun> recentRuns = collectionRunRepository.findTop50ByOrderByStartTimeDesc();
        List<com.boredintern.lead_discovery.model.Lead> recentLeads =
                leadRepository.findTop25ByIntentLevelAndSourcePlatformOrderByDiscoveredAtDesc(
                        IntentLevel.HIGH,
                        SourcePlatform.LINKEDIN
                );

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
