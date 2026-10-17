package com.boredintern.lead_discovery.collector;

import com.boredintern.lead_discovery.model.Campaign;
import com.boredintern.lead_discovery.model.SourcePlatform;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Component
public class HackerNewsCollector implements LeadSourceCollector {

    private static final String BASE_URL = "https://hn.algolia.com/api/v1/search_by_date";
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public HackerNewsCollector(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    @Override
    public SourcePlatform getPlatform() {
        return SourcePlatform.HACKER_NEWS;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public List<RawLeadPayload> collect(Campaign campaign, RunLogger logger) {
        List<RawLeadPayload> results = new ArrayList<>();
        List<String> queries = resolveQueries(campaign);

        logger.info(String.format("HackerNews: Starting collection for %d queries", queries.size()));

        for (String query : queries) {
            try {
                String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
                String url = BASE_URL + "?query=" + encodedQuery + "&tags=(story,comment)&hitsPerPage=20";

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("User-Agent", "LeadDiscoverySystem/1.0 (HN Algolia Ingest)")
                        .header("Accept", "application/json")
                        .timeout(Duration.ofSeconds(12))
                        .GET()
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    JsonNode root = objectMapper.readTree(response.body());
                    JsonNode hits = root.path("hits");
                    if (hits.isArray()) {
                        int count = 0;
                        for (JsonNode hit : hits) {
                            String objectId = hit.path("objectID").asText();
                            String title = hit.path("title").asText(null);
                            String storyTitle = hit.path("story_title").asText(null);
                            String commentText = hit.path("comment_text").asText(null);
                            String storyText = hit.path("story_text").asText(null);
                            String author = hit.path("author").asText(null);
                            String createdAtStr = hit.path("created_at").asText(null);
                            String itemUrl = "https://news.ycombinator.com/item?id=" + objectId;

                            String effectiveTitle = title != null && !title.isBlank() ? title : (storyTitle != null ? "HN: " + storyTitle : "HN Post #" + objectId);
                            String effectiveContent = commentText != null && !commentText.isBlank() ? commentText : (storyText != null ? storyText : effectiveTitle);

                            Instant publishedAt = Instant.now();
                            if (createdAtStr != null && !createdAtStr.isBlank()) {
                                try {
                                    publishedAt = Instant.parse(createdAtStr);
                                } catch (Exception ignored) {
                                }
                            }

                            results.add(new RawLeadPayload(
                                    effectiveTitle,
                                    effectiveContent,
                                    itemUrl,
                                    SourcePlatform.HACKER_NEWS,
                                    author,
                                    author != null ? "https://news.ycombinator.com/user?id=" + author : null,
                                    null,
                                    null,
                                    publishedAt,
                                    Map.of("objectID", objectId, "query", query)
                            ));
                            count++;
                        }
                        logger.info(String.format("HackerNews: Found %d posts for query '%s'", count, query));
                    }
                } else {
                    logger.warn(String.format("HackerNews: Non-200 status %d for query '%s'", response.statusCode(), query));
                }

                // Polite pacing between queries
                Thread.sleep(1000);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("HackerNews collection interrupted", e);
                break;
            } catch (Exception e) {
                logger.error("HackerNews error querying '" + query + "'", e);
            }
        }

        return results;
    }

    private List<String> resolveQueries(Campaign campaign) {
        Set<String> queries = new LinkedHashSet<>();
        if (campaign != null && campaign.getKeywords() != null && !campaign.getKeywords().isBlank()) {
            String[] parts = campaign.getKeywords().split("[,;\\n]+");
            for (String p : parts) {
                String clean = p.trim();
                if (!clean.isEmpty()) {
                    queries.add(clean);
                }
                if (queries.size() >= 3) break;
            }
        }
        if (queries.isEmpty()) {
            queries.add("hiring");
            queries.add("freelance");
            queries.add("seeking developer");
        }
        return new ArrayList<>(queries);
    }
}
