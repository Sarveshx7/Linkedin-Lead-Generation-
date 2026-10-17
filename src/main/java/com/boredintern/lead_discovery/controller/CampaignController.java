package com.boredintern.lead_discovery.controller;

import com.boredintern.lead_discovery.model.Campaign;
import com.boredintern.lead_discovery.model.CollectionRun;
import com.boredintern.lead_discovery.repository.CampaignRepository;
import com.boredintern.lead_discovery.service.LeadCollectionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/campaigns")
@CrossOrigin(origins = "*")
public class CampaignController {

    private final CampaignRepository campaignRepository;
    private final LeadCollectionService collectionService;

    public CampaignController(CampaignRepository campaignRepository, LeadCollectionService collectionService) {
        this.campaignRepository = campaignRepository;
        this.collectionService = collectionService;
    }

    @GetMapping
    public ResponseEntity<List<Campaign>> getAllCampaigns() {
        return ResponseEntity.ok(campaignRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Campaign> getCampaignById(@PathVariable Long id) {
        return campaignRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Campaign> createCampaign(@RequestBody Campaign campaign) {
        campaign.setCreatedAt(Instant.now());
        campaign.setEnabledSources("LINKEDIN");
        campaign.setCustomRssFeeds(null);
        if (campaign.getIsActive() == null) {
            campaign.setIsActive(true);
        }
        return ResponseEntity.ok(campaignRepository.save(campaign));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Campaign> updateCampaign(@PathVariable Long id, @RequestBody Campaign campaignDetails) {
        return campaignRepository.findById(id).map(campaign -> {
            campaign.setName(campaignDetails.getName());
            campaign.setDescription(campaignDetails.getDescription());
            campaign.setKeywords(campaignDetails.getKeywords());
            campaign.setExcludedKeywords(campaignDetails.getExcludedKeywords());
            campaign.setTargetIndianMarket(campaignDetails.getTargetIndianMarket());
            campaign.setEnabledSources("LINKEDIN");
            campaign.setCustomRssFeeds(null);
            if (campaignDetails.getIsActive() != null) {
                campaign.setIsActive(campaignDetails.getIsActive());
            }
            return ResponseEntity.ok(campaignRepository.save(campaign));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCampaign(@PathVariable Long id) {
        if (campaignRepository.existsById(id)) {
            campaignRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/{id}/run")
    public ResponseEntity<CollectionRun> triggerCampaignRun(@PathVariable Long id) {
        CollectionRun run = collectionService.executeCollectionSync(id);
        return ResponseEntity.ok(run);
    }
}
