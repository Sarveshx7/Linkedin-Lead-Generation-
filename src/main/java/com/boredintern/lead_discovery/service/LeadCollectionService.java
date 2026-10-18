package com.boredintern.lead_discovery.service;

import com.boredintern.lead_discovery.collector.LeadSourceCollector;
import com.boredintern.lead_discovery.collector.RawLeadPayload;
import com.boredintern.lead_discovery.collector.RunLogger;
import com.boredintern.lead_discovery.model.*;
import com.boredintern.lead_discovery.repository.CampaignRepository;
import com.boredintern.lead_discovery.repository.CollectionRunRepository;
import com.boredintern.lead_discovery.repository.LeadRepository;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
public class LeadCollectionService {

    private static final Map<String, String> DEFAULT_LINKEDIN_KEYWORDS = Map.of(
            "LinkedIn: Influencer Marketing Leads",
            "influencer, influencers, content creator, content creators, UGC, creator collaboration, brand collaboration, brand sponsorship, brand ambassador, sponsored content, paid partnership, product launch",
            "LinkedIn: Advertising & Creative Leads",
            "advertising agency, creative agency, social media campaign, digital marketing agency, marketing agency, brand campaign, campaign partner, marketing RFP, advertising campaign, media buying, promotional campaign, brand collaboration",
            "LinkedIn: Production House Leads",
            "production house, video production, brand video, product video, commercial shoot, product shoot, video production team, corporate film, ad film, videographer, production partner, content production",
            "LinkedIn: Brand Film Leads",
            "brand film, corporate film, product commercial, brand video, ad film, TVC, video campaign, storytelling campaign, company profile video, product film, production house, creative team"
    );

    private static final Map<String, String> LEGACY_LINKEDIN_KEYWORDS = Map.of(
            "LinkedIn: Influencer Marketing Leads",
            "influencer marketing agency, looking for influencers, creator agency, influencer campaign, D2C brand, creators",
            "LinkedIn: Advertising & Creative Leads",
            "advertising agency, searching for an agency, ad campaign, creative agency, media buying, promotional services, pitch invited",
            "LinkedIn: Production House Leads",
            "production house, recommend a production house, commercial shoot, product shoot, video production team, shoot team, videographer",
            "LinkedIn: Brand Film Leads",
            "brand film, produce a brand film, corporate film, product commercial, creative team brand film, TVC, brand video"
    );

    private static final String PREVIOUS_DEFAULT_INFLUENCER_KEYWORDS =
            "looking for a content creator, looking for content creators, looking for an influencer, looking for influencers, brand collab, creator partnership, UGC creator, social media creator, paid partnership, sponsored content, brand ambassador, influencer campaign";

    private final List<LeadSourceCollector> collectors;
    private final LeadRepository leadRepository;
    private final CampaignRepository campaignRepository;
    private final CollectionRunRepository collectionRunRepository;
    private final DeduplicationService deduplicationService;
    private final SanitizationService sanitizationService;
    private final CategorizationService categorizationService;
    private final DataSource dataSource;

    public LeadCollectionService(
            List<LeadSourceCollector> collectors,
            LeadRepository leadRepository,
            CampaignRepository campaignRepository,
            CollectionRunRepository collectionRunRepository,
            DeduplicationService deduplicationService,
            SanitizationService sanitizationService,
            CategorizationService categorizationService,
            DataSource dataSource
    ) {
        this.collectors = collectors;
        this.leadRepository = leadRepository;
        this.campaignRepository = campaignRepository;
        this.collectionRunRepository = collectionRunRepository;
        this.deduplicationService = deduplicationService;
        this.sanitizationService = sanitizationService;
        this.categorizationService = categorizationService;
        this.dataSource = dataSource;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        // 0. Remove stale enum constraints left by older schema versions.
        dropCheckConstraints();

        // 1. Recover any runs interrupted by server restart
        List<CollectionRun> hangingRuns = collectionRunRepository.findByStatus(RunStatus.RUNNING);
        for (CollectionRun run : hangingRuns) {
            run.setStatus(RunStatus.INTERRUPTED);
            run.setEndTime(Instant.now());
            String existingLogs = run.getErrorLogs() != null ? run.getErrorLogs() : "";
            run.setErrorLogs(existingLogs + "\n[SYSTEM] Run interrupted due to server restart.");
            collectionRunRepository.save(run);
        }

        // 2. Ensure LinkedIn target campaigns are seeded
        ensureLinkedInCampaignsSeeded();
    }

    public void dropCheckConstraints() {
        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("ALTER TABLE leads DROP CONSTRAINT IF EXISTS leads_service_category_check");
            stmt.execute("ALTER TABLE leads DROP CONSTRAINT IF EXISTS leads_source_platform_check");
            stmt.execute("ALTER TABLE leads DROP CONSTRAINT IF EXISTS leads_intent_level_check");
            stmt.execute("ALTER TABLE leads DROP CONSTRAINT IF EXISTS leads_review_status_check");

            int droppedConstraints = 0;
            for (String tableName : List.of("leads", "collection_runs")) {
                try (PreparedStatement findChecks = conn.prepareStatement(
                        "SELECT constraint_name FROM information_schema.table_constraints " +
                        "WHERE UPPER(table_schema) = UPPER(CURRENT_SCHEMA()) " +
                        "AND UPPER(table_name) = UPPER(?) AND UPPER(constraint_type) = 'CHECK'")) {
                    findChecks.setString(1, tableName);
                    try (var rs = findChecks.executeQuery()) {
                    List<String> toDrop = new ArrayList<>();
                    while (rs.next()) {
                        String cName = rs.getString(1);
                        if (cName != null && !cName.endsWith("_not_null")) {
                            toDrop.add(cName);
                        }
                    }
                    for (String cName : toDrop) {
                        try (Statement dropStmt = conn.createStatement()) {
                            String quotedConstraint = "\"" + cName.replace("\"", "\"\"") + "\"";
                            dropStmt.execute("ALTER TABLE " + tableName + " DROP CONSTRAINT IF EXISTS " + quotedConstraint);
                            droppedConstraints++;
                        }
                    }
                    }
                }
            }
            System.out.println("[SYSTEM] Removed " + droppedConstraints + " stale check constraint(s) from lead and collection-run tables.");
        } catch (Exception e) {
            throw new IllegalStateException("Could not remove stale lead collection check constraints", e);
        }
    }

    public void ensureLinkedInCampaignsSeeded() {
        boolean hasLinkedIn = campaignRepository.findAll().stream()
                .anyMatch(c -> c.getName() != null && c.getName().contains("LinkedIn"));

        if (!hasLinkedIn) {
            campaignRepository.deleteAll(); // Clean up old tech campaigns

            Campaign c1 = new Campaign();
            c1.setName("LinkedIn: Influencer Marketing Leads");
            c1.setDescription("Brand and company posts seeking social creators, influencers, UGC, or paid collaboration partners.");
            c1.setKeywords(DEFAULT_LINKEDIN_KEYWORDS.get(c1.getName()));
            c1.setExcludedKeywords("spam, crypto, follow for follow");
            c1.setTargetIndianMarket(true);
            c1.setEnabledSources("LINKEDIN");
            c1.setIsActive(true);
            campaignRepository.save(c1);

            Campaign c2 = new Campaign();
            c2.setName("LinkedIn: Advertising & Creative Leads");
            c2.setDescription("Companies seeking advertising, digital marketing, social media campaign, or creative agency partners.");
            c2.setKeywords(DEFAULT_LINKEDIN_KEYWORDS.get(c2.getName()));
            c2.setExcludedKeywords("job vacancy, intern");
            c2.setTargetIndianMarket(true);
            c2.setEnabledSources("LINKEDIN");
            c2.setIsActive(true);
            campaignRepository.save(c2);

            Campaign c3 = new Campaign();
            c3.setName("LinkedIn: Production House Leads");
            c3.setDescription("Businesses looking for video production teams, commercial shoots, or professional content production.");
            c3.setKeywords(DEFAULT_LINKEDIN_KEYWORDS.get(c3.getName()));
            c3.setExcludedKeywords("actor audition, acting class");
            c3.setTargetIndianMarket(true);
            c3.setEnabledSources("LINKEDIN");
            c3.setIsActive(true);
            campaignRepository.save(c3);

            Campaign c4 = new Campaign();
            c4.setName("LinkedIn: Brand Film Leads");
            c4.setDescription("Companies looking for brand videos, corporate films, product commercials, or storytelling campaigns.");
            c4.setKeywords(DEFAULT_LINKEDIN_KEYWORDS.get(c4.getName()));
            c4.setExcludedKeywords("movie review, torrent");
            c4.setTargetIndianMarket(true);
            c4.setEnabledSources("LINKEDIN");
            c4.setIsActive(true);
            campaignRepository.save(c4);
        }

        refreshUnmodifiedDefaultCampaignKeywords();
    }

    private void refreshUnmodifiedDefaultCampaignKeywords() {
        for (Campaign campaign : campaignRepository.findAll()) {
            String campaignName = campaign.getName();
            if (campaignName == null) continue;
            String updatedDefaults = DEFAULT_LINKEDIN_KEYWORDS.get(campaignName);
            if ("LinkedIn: Influencer Marketing Leads".equals(campaignName)
                    && PREVIOUS_DEFAULT_INFLUENCER_KEYWORDS.equals(campaign.getKeywords())) {
                campaign.setKeywords(updatedDefaults);
                campaignRepository.save(campaign);
                continue;
            }
            String previousDefaults = LEGACY_LINKEDIN_KEYWORDS.get(campaignName);
            if (previousDefaults != null && updatedDefaults != null
                    && previousDefaults.equals(campaign.getKeywords())) {
                campaign.setKeywords(updatedDefaults);
                campaignRepository.save(campaign);
            }
        }
    }

    public void purgeAndCollectLinkedInOnly() {
        dropCheckConstraints();
        leadRepository.deleteAll();
        collectionRunRepository.deleteAll();
        ensureLinkedInCampaignsSeeded();
        executeCollectionSync(null);
    }

    public CollectionRun executeCollectionSync(Long campaignId) {
        boolean globalRun = campaignId == null;
        List<Campaign> campaignsToRun;
        if (globalRun) {
            campaignsToRun = campaignRepository.findByIsActiveTrue();
            if (campaignsToRun.isEmpty()) {
                campaignsToRun = Collections.singletonList(null);
            }
        } else {
            Campaign selectedCampaign = campaignRepository.findById(campaignId).orElse(null);
            campaignsToRun = selectedCampaign == null
                    ? Collections.emptyList()
                    : List.of(selectedCampaign);
        }

        CollectionRun run = new CollectionRun();
        run.setCampaignId(globalRun ? null : campaignId);
        run.setCampaignName(globalRun
                ? "Global Collection Run"
                : campaignsToRun.isEmpty() ? "Missing Campaign #" + campaignId : campaignsToRun.get(0).getName());
        run.setStatus(RunStatus.RUNNING);
        run.setStartTime(Instant.now());
        run = collectionRunRepository.save(run);

        RunLogger logger = new RunLogger();
        logger.info("Collection run started for campaign: " + run.getCampaignName());

        int discovered = 0;
        int duplicates = 0;
        int outsideIndiaSkipped = 0;
        int sourcesCount = 0;
        boolean hadErrors = false;

        if (campaignsToRun.isEmpty()) {
            logger.error("Requested campaign does not exist.", null);
            hadErrors = true;
        }

        for (Campaign campaign : campaignsToRun) {
            if (campaign != null) {
                logger.info("Collecting campaign: " + campaign.getName());
            }

            // Restrict collection to public LinkedIn results returned by Tavily, Exa,
            // and Serper, even if an older campaign record lists other sources.
            Set<String> enabledPlatforms = Set.of("LINKEDIN");

            for (LeadSourceCollector collector : collectors) {
                String platformName = collector.getPlatform().name();
                if (!enabledPlatforms.isEmpty() && !enabledPlatforms.contains(platformName)) {
                    logger.info("Collector " + platformName + " is disabled for this campaign. Skipping.");
                    continue;
                }

                sourcesCount++;
                if (!collector.isAvailable()) {
                    if (platformName.equals("LINKEDIN")) {
                        logger.warn("LinkedIn search is not configured. Add one or more backend keys: TAVILY_API_KEY, EXA_API_KEY, SERPER_API_KEY.");
                    } else {
                        logger.warn("Collector " + platformName + " is not configured and was skipped.");
                    }
                    hadErrors = true;
                    continue;
                }

                try {
                    logger.info("Executing collector: " + platformName);
                    List<RawLeadPayload> rawPayloads = collector.collect(campaign, logger);

                    for (RawLeadPayload raw : rawPayloads) {
                        try {
                            String canonicalUrl = deduplicationService.canonicalizeUrl(raw.sourceUrl());
                            String cleanTitle = sanitizationService.sanitizeHtml(raw.title());
                            String cleanContent = sanitizationService.sanitizeHtml(raw.content());

                            String fingerprint = deduplicationService.computeFingerprint(cleanTitle, cleanContent, canonicalUrl);

                            if (deduplicationService.isDuplicate(fingerprint)) {
                                duplicates++;
                                continue;
                            }

                            CategorizationService.CategorizationResult cat = categorizationService.analyze(
                                    cleanTitle, cleanContent, raw.author(), canonicalUrl, raw.location()
                            );
                            if (!cat.isIndianMarket()) {
                                outsideIndiaSkipped++;
                                continue;
                            }
                            String contactInfo = sanitizationService.extractContactInfo(cleanContent);

                            Lead lead = new Lead();
                            lead.setFingerprint(fingerprint);
                            lead.setTitle(cleanTitle.length() > 950 ? cleanTitle.substring(0, 950) + "..." : cleanTitle);
                            lead.setContent(cleanContent);
                            lead.setSourceUrl(raw.sourceUrl());
                            lead.setSourcePlatform(raw.sourcePlatform());
                            lead.setAuthor(raw.author());
                            lead.setAuthorProfileUrl(raw.authorProfileUrl());
                            lead.setCompany(raw.company());
                            lead.setLocation(raw.location());
                            lead.setPublishedAt(raw.publishedAt());
                            lead.setDiscoveredAt(Instant.now());
                            lead.setServiceCategory(cat.category());
                            lead.setIntentLevel(cat.intentLevel());
                            lead.setIntentReasoning(cat.intentReasoning());
                            lead.setIsIndianMarket(cat.isIndianMarket());
                            lead.setRelevanceScore(cat.relevanceScore());
                            lead.setCompletenessScore(cat.completenessScore());
                            lead.setDetectedKeywords(cat.detectedKeywords());
                            lead.setContactInfo(contactInfo);
                            Map<String, String> providerMetadata = raw.rawMetadata() == null ? Map.of() : raw.rawMetadata();
                            lead.setSearchProviders(providerMetadata.get("searchProviders"));
                            lead.setProviderMatchCount(parseInteger(providerMetadata.get("providerMatchCount")));
                            lead.setProviderCount(parseInteger(providerMetadata.get("providerCount")));
                            lead.setReviewStatus(ReviewStatus.DISCOVERED);
                            lead.setCampaignId(campaign != null ? campaign.getId() : null);

                            leadRepository.save(lead);
                            discovered++;

                        } catch (Exception e) {
                            logger.error("Failed to save lead item: " + raw.title(), e);
                            hadErrors = true;
                        }
                    }

                } catch (Exception e) {
                    hadErrors = true;
                    logger.error("Collector " + platformName + " failed with unexpected error", e);
                }
            }

            if (campaign != null) {
                campaign.setLastRunAt(Instant.now());
                campaignRepository.save(campaign);
            }
        }

        run.setEndTime(Instant.now());
        run.setLeadsDiscovered(discovered);
        run.setDuplicatesSkipped(duplicates);
        run.setSourcesAttempted(sourcesCount);
        run.setErrorLogs(logger.getFormattedLogs());
        run.setStatus(hadErrors ? (discovered > 0 ? RunStatus.PARTIAL_SUCCESS : RunStatus.FAILED) : RunStatus.COMPLETED);

        if (outsideIndiaSkipped > 0) {
            logger.info("Excluded " + outsideIndiaSkipped + " post(s) without explicit India-related location or market wording.");
            run.setErrorLogs(logger.getFormattedLogs());
        }

        logger.info(String.format("Run completed. Discovered: %d, Duplicates: %d, Status: %s",
                discovered, duplicates, run.getStatus()));

        return collectionRunRepository.save(run);
    }

    public CompletableFuture<CollectionRun> triggerAsyncRun(Long campaignId) {
        return CompletableFuture.supplyAsync(() -> executeCollectionSync(campaignId));
    }

    private Integer parseInteger(String value) {
        if (value == null || value.isBlank()) return 0;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }
}
