package com.boredintern.lead_discovery.dto;

import com.boredintern.lead_discovery.model.CollectionRun;
import com.boredintern.lead_discovery.model.Lead;

import java.util.List;
import java.util.Map;

public record DashboardStatsDto(
        long totalLeads,
        long highIntentLeads,
        long indianMarketLeads,
        long shortlistedLeads,
        long activeCampaigns,
        long totalRuns,
        Map<String, Long> categoryCounts,
        Map<String, Long> intentCounts,
        Map<String, Long> platformCounts,
        List<CollectionRun> recentRuns,
        List<Lead> recentLeads
) {
}
