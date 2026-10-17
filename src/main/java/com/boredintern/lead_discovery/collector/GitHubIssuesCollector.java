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
public class GitHubIssuesCollector implements LeadSourceCollector {

    private static final String API_BASE = "https://api.github.com/search/issues";
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public GitHubIssuesCollector(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    @Override
    public SourcePlatform getPlatform() {
        return SourcePlatform.GITHUB;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public List<RawLeadPayload> collect(Campaign campaign, RunLogger logger) {
        List<RawLeadPayload> results = new ArrayList<>();
        List<String> queries = resolveQueries(campaign);

        logger.info(String.format("GitHub: Starting collection for %d queries", queries.size()));

        for (String query : queries) {
            try {
                String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
                String url = API_BASE + "?q=" + encoded + "&per_page=15&sort=created&order=desc";

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("User-Agent", "LeadDiscoverySystem/1.0 (GitHub Issues Ingest)")
                        .header("Accept", "application/vnd.github+json")
                        .timeout(Duration.ofSeconds(12))
                        .GET()
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                // Monitor rate limits
                Optional<String> remaining = response.headers().firstValue("x-ratelimit-remaining");
                remaining.ifPresent(r -> logger.info("GitHub: Rate limit remaining: " + r));

                if (response.statusCode() == 200) {
                    JsonNode root = objectMapper.readTree(response.body());
                    JsonNode items = root.path("items");
                    if (items.isArray()) {
                        int count = 0;
                        for (JsonNode item : items) {
                            String title = item.path("title").asText("");
                            String body = item.path("body").asText("");
                            String htmlUrl = item.path("html_url").asText("");
                            String createdAtStr = item.path("created_at").asText(null);

                            JsonNode userNode = item.path("user");
                            String login = userNode.path("login").asText(null);
                            String profileUrl = userNode.path("html_url").asText(null);

                            Instant publishedAt = Instant.now();
                            if (createdAtStr != null && !createdAtStr.isBlank()) {
                                try {
                                    publishedAt = Instant.parse(createdAtStr);
                                } catch (Exception ignored) {
                                }
                            }

                            if (!title.isBlank() && !htmlUrl.isBlank()) {
                                results.add(new RawLeadPayload(
                                        title,
                                        body.isBlank() ? title : body,
                                        htmlUrl,
                                        SourcePlatform.GITHUB,
                                        login,
                                        profileUrl,
                                        null,
                                        null,
                                        publishedAt,
                                        Map.of("query", query)
                                ));
                                count++;
                            }
                        }
                        logger.info(String.format("GitHub: Found %d issues for query '%s'", count, query));
                    }
                } else if (response.statusCode() == 403) {
                    logger.warn("GitHub: Rate limit reached (HTTP 403). Pacing back.");
                    break;
                } else {
                    logger.warn(String.format("GitHub: HTTP %d for query '%s'", response.statusCode(), query));
                }

                Thread.sleep(1500);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("GitHub collection interrupted", e);
                break;
            } catch (Exception e) {
                logger.error("GitHub error querying '" + query + "'", e);
            }
        }

        return results;
    }

    private List<String> resolveQueries(Campaign campaign) {
        if (campaign != null && Boolean.TRUE.equals(campaign.getTargetIndianMarket())) {
            return List.of("freelance india", "hiring developer india");
        }
        return List.of("freelance contract", "hiring developer");
    }
}
