package com.boredintern.lead_discovery.service;

import com.boredintern.lead_discovery.model.Lead;
import com.boredintern.lead_discovery.repository.LeadRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Re-evaluates existing rows after the India-signal rules are tightened. */
@Component
public class IndiaSignalRecheck implements ApplicationRunner {

    private final LeadRepository leadRepository;
    private final CategorizationService categorizationService;

    public IndiaSignalRecheck(LeadRepository leadRepository, CategorizationService categorizationService) {
        this.leadRepository = leadRepository;
        this.categorizationService = categorizationService;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<Lead> leads = leadRepository.findAll();
        boolean changed = false;
        for (Lead lead : leads) {
            CategorizationService.CategorizationResult result = categorizationService.analyze(
                    lead.getTitle(), lead.getContent(), lead.getAuthor(), lead.getSourceUrl(), lead.getLocation()
            );
            boolean rowChanged = !Boolean.valueOf(result.isIndianMarket()).equals(lead.getIsIndianMarket())
                    || !java.util.Objects.equals(result.relevanceScore(), lead.getRelevanceScore())
                    || !java.util.Objects.equals(result.completenessScore(), lead.getCompletenessScore())
                    || !java.util.Objects.equals(result.detectedKeywords(), lead.getDetectedKeywords());
            if (rowChanged) {
                lead.setIsIndianMarket(result.isIndianMarket());
                lead.setRelevanceScore(result.relevanceScore());
                lead.setCompletenessScore(result.completenessScore());
                lead.setDetectedKeywords(result.detectedKeywords());
                changed = true;
            }
        }
        if (changed) leadRepository.saveAll(leads);
    }
}
