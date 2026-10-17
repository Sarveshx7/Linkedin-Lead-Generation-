package com.boredintern.lead_discovery.collector;

import com.boredintern.lead_discovery.model.Campaign;
import com.boredintern.lead_discovery.model.SourcePlatform;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
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
public class RedditOAuthCollector implements LeadSourceCollector {

    @Value("${lead.collector.reddit.client-id:}")
    private String clientId;

    @Value("${lead.collector.reddit.client-secret:}")
    private String clientSecret;

    @Value("${lead.collector.reddit.user-agent:LeadDiscoverySystem/1.0}")
    private String userAgent;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    private String cachedToken;
    private Instant tokenExpiry = Instant.EPOCH;

    public RedditOAuthCollector(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    @Override
    public SourcePlatform getPlatform() {
        return SourcePlatform.REDDIT;
    }

    @Override
    public boolean isAvailable() {
        return clientId != null && !clientId.isBlank() && clientSecret != null && !clientSecret.isBlank();
    }

    @Override
    public List<RawLeadPayload> collect(Campaign campaign, RunLogger logger) {
        if (!isAvailable()) {
            logger.info("Reddit: Official API credentials not configured. Skipping Reddit without blocking.");
            return Collections.emptyList();
        }

        List<RawLeadPayload> results = new ArrayList<>();
        try {
            String token = getOrFetchAccessToken();
            if (token == null) {
                logger.warn("Reddit: Unable to acquire OAuth access token.");
                return results;
            }

            List<String> subreddits = List.of("developersIndia", "IndianStartups", "forhire");
            String query = (campaign != null && campaign.getKeywords() != null && !campaign.getKeywords().isBlank())
                    ? campaign.getKeywords().split(",")[0].trim() : "hiring";

            logger.info(String.format("Reddit: Starting collection across %d subreddits for query '%s'", subreddits.size(), query));

            for (String sub : subreddits) {
                String searchUrl = String.format("https://oauth.reddit.com/r/%s/search?q=%s&restrict_sr=1&sort=new&limit=15",
                        sub, URLEncoder.encode(query, StandardCharsets.UTF_8));

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(searchUrl))
                        .header("Authorization", "Bearer " + token)
                        .header("User-Agent", userAgent)
                        .timeout(Duration.ofSeconds(12))
                        .GET()
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    JsonNode root = objectMapper.readTree(response.body());
                    JsonNode children = root.path("data").path("children");
                    int count = 0;
                    if (children.isArray()) {
                        for (JsonNode child : children) {
                            JsonNode postData = child.path("data");
                            String title = postData.path("title").asText("");
                            String selftext = postData.path("selftext").asText("");
                            String permalink = postData.path("permalink").asText("");
                            String author = postData.path("author").asText("");
                            long createdUtc = postData.path("created_utc").asLong(0);

                            String postUrl = permalink.startsWith("http") ? permalink : "https://www.reddit.com" + permalink;
                            Instant publishedAt = createdUtc > 0 ? Instant.ofEpochSecond(createdUtc) : Instant.now();

                            if (!title.isBlank()) {
                                results.add(new RawLeadPayload(
                                        title,
                                        selftext.isBlank() ? title : selftext,
                                        postUrl,
                                        SourcePlatform.REDDIT,
                                        author,
                                        "https://www.reddit.com/user/" + author,
                                        "r/" + sub,
                                        null,
                                        publishedAt,
                                        Map.of("subreddit", sub)
                                ));
                                count++;
                            }
                        }
                    }
                    logger.info(String.format("Reddit: Ingested %d posts from r/%s", count, sub));
                } else {
                    logger.warn(String.format("Reddit: HTTP %d when querying r/%s", response.statusCode(), sub));
                }

                Thread.sleep(1000);
            }

        } catch (Exception e) {
            logger.error("Reddit collection encountered an error", e);
        }

        return results;
    }

    private synchronized String getOrFetchAccessToken() {
        if (cachedToken != null && Instant.now().isBefore(tokenExpiry.minusSeconds(60))) {
            return cachedToken;
        }

        try {
            String authHeader = "Basic " + Base64.getEncoder().encodeToString(
                    (clientId + ":" + clientSecret).getBytes(StandardCharsets.UTF_8));

            String form = "grant_type=client_credentials";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://www.reddit.com/api/v1/access_token"))
                    .header("Authorization", authHeader)
                    .header("User-Agent", userAgent)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .timeout(Duration.ofSeconds(10))
                    .POST(HttpRequest.BodyPublishers.ofString(form))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JsonNode json = objectMapper.readTree(response.body());
                cachedToken = json.path("access_token").asText(null);
                long expiresIn = json.path("expires_in").asLong(3600);
                tokenExpiry = Instant.now().plusSeconds(expiresIn);
                return cachedToken;
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}
