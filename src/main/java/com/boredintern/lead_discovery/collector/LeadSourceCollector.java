package com.boredintern.lead_discovery.collector;

import com.boredintern.lead_discovery.model.Campaign;
import com.boredintern.lead_discovery.model.SourcePlatform;

import java.util.List;

public interface LeadSourceCollector {

    SourcePlatform getPlatform();

    boolean isAvailable();

    List<RawLeadPayload> collect(Campaign campaign, RunLogger logger);
}
