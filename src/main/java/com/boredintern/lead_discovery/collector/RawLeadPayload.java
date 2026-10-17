package com.boredintern.lead_discovery.collector;

import com.boredintern.lead_discovery.model.SourcePlatform;
import java.time.Instant;
import java.util.Map;

public record RawLeadPayload(
    String title,
    String content,
    String sourceUrl,
    SourcePlatform sourcePlatform,
    String author,
    String authorProfileUrl,
    String company,
    String location,
    Instant publishedAt,
    Map<String, String> rawMetadata
) {
}
