package com.boredintern.lead_discovery.collector;

import com.boredintern.lead_discovery.model.Campaign;
import com.boredintern.lead_discovery.model.SourcePlatform;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Component
public class DevToCollector implements LeadSourceCollector {

    private static final String API_BASE = "https://dev.to/api/articles";
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public DevToCollector(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    @Override
    public SourcePlatform getPlatform() {
        return SourcePlatform.DEV_TO;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public List<RawLeadPayload> collect(Campaign campaign, RunLogger logger) {
        List<RawLeadPayload> results = new ArrayList<>();
        List<String> tags = List.of("hiring", "freelance", "jobs", "contract", "india");

        logger.info(String.format("Dev.to: Starting collection across %d tags", tags.size()));

        for (String tag : tags) {
            try {
                String url = API_BASE + "?tag=" + tag + "&per_page=15";
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("User-Agent", "LeadDiscoverySystem/1.0 (DevTo Ingest)")
                        .header("Accept", "application/json")
                        .timeout(Duration.ofSeconds(12))
                        .GET()
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    JsonNode array = objectMapper.readTree(response.body());
                    if (array.isArray()) {
                        int count = 0;
                        for (JsonNode item : array) {
                            String title = item.path("title").asText("");
                            String description = item.path("description").asText("");
                            String itemUrl = item.path("url").asText("");
                            String publishedAtStr = item.path("published_at").asText(null);

                            JsonNode userNode = item.path("user");
                            String authorName = userNode.path("name").asText(null);
                            String authorUsername = userNode.path("username").asText(null);
                            String authorProfile = authorUsername != null ? "https://dev.to/" + authorUsername : null;

                            JsonNode orgNode = item.path("organization");
                            String company = orgNode.path("name").asText(null);

                            Instant publishedAt = Instant.now();
                            if (publishedAtStr != null && !publishedAtStr.isBlank()) {
                                try {
                                    publishedAt = Instant.parse(publishedAtStr);
                                } catch (Exception ignored) {
                                }
                            }

                            if (!title.isBlank() && !itemUrl.isBlank()) {
                                results.add(new RawLeadPayload(
                                        title,
                                        description.isBlank() ? title : description,
                                        itemUrl,
                                        SourcePlatform.DEV_TO,
                                        authorName,
                                        authorProfile,
                                        company,
                                        null,
                                        publishedAt,
                                        Map.of("tag", tag)
                                ));
                                count++;
                            }
                        }
                        logger.info(String.format("Dev.to: Found %d posts for tag '%s'", count, tag));
                    }
                } else {
                    logger.warn(String.format("Dev.to: Received status %d for tag '%s'", response.statusCode(), tag));
                }

                Thread.sleep(1000);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("Dev.to collection interrupted", e);
                break;
            } catch (Exception e) {
                logger.error("Dev.to error querying tag '" + tag + "'", e);
            }
        }

        return results;
    }
}
