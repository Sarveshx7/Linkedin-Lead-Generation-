package com.boredintern.lead_discovery.collector;

import com.boredintern.lead_discovery.model.Campaign;
import com.boredintern.lead_discovery.model.SourcePlatform;
import com.rometools.rome.feed.synd.SyndContent;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import org.springframework.stereotype.Component;

import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Component
public class RssSyndicationCollector implements LeadSourceCollector {

    private final HttpClient httpClient;

    private static final List<String> DEFAULT_FEEDS = List.of(
            "https://weworkremotely.com/categories/remote-programming-jobs.rss",
            "https://remoteok.com/remote-jobs.rss",
            "https://dev.to/feed/tag/hiring"
    );

    public RssSyndicationCollector() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    @Override
    public SourcePlatform getPlatform() {
        return SourcePlatform.RSS_FEED;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public List<RawLeadPayload> collect(Campaign campaign, RunLogger logger) {
        List<RawLeadPayload> results = new ArrayList<>();
        Set<String> feedUrls = new LinkedHashSet<>(DEFAULT_FEEDS);

        if (campaign != null && campaign.getCustomRssFeeds() != null && !campaign.getCustomRssFeeds().isBlank()) {
            String[] customs = campaign.getCustomRssFeeds().split("[\\r\\n,;]+");
            for (String c : customs) {
                String trimmed = c.trim();
                if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
                    feedUrls.add(trimmed);
                }
            }
        }

        logger.info(String.format("RSS: Starting ingestion across %d syndication feeds", feedUrls.size()));

        for (String feedUrl : feedUrls) {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(feedUrl))
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) LeadDiscoveryFeedReader/1.0")
                        .header("Accept", "application/rss+xml, application/atom+xml, application/xml, text/xml")
                        .timeout(Duration.ofSeconds(12))
                        .GET()
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200 && response.body() != null && !response.body().isBlank()) {
                    SyndFeedInput input = new SyndFeedInput();
                    SyndFeed feed = input.build(new StringReader(response.body()));

                    int count = 0;
                    for (SyndEntry entry : feed.getEntries()) {
                        String title = entry.getTitle() != null ? entry.getTitle().trim() : "";
                        String link = entry.getLink() != null ? entry.getLink().trim() : "";
                        String author = entry.getAuthor() != null ? entry.getAuthor().trim() : null;

                        StringBuilder contentBuilder = new StringBuilder();
                        if (entry.getDescription() != null && entry.getDescription().getValue() != null) {
                            contentBuilder.append(entry.getDescription().getValue());
                        }
                        if (entry.getContents() != null && !entry.getContents().isEmpty()) {
                            for (SyndContent sc : entry.getContents()) {
                                if (sc.getValue() != null) {
                                    contentBuilder.append("\n").append(sc.getValue());
                                }
                            }
                        }

                        String content = contentBuilder.toString().trim();
                        if (content.isEmpty()) {
                            content = title;
                        }

                        Instant publishedAt = Instant.now();
                        if (entry.getPublishedDate() != null) {
                            publishedAt = entry.getPublishedDate().toInstant();
                        } else if (entry.getUpdatedDate() != null) {
                            publishedAt = entry.getUpdatedDate().toInstant();
                        }

                        if (!title.isEmpty() && !link.isEmpty()) {
                            results.add(new RawLeadPayload(
                                    title,
                                    content,
                                    link,
                                    SourcePlatform.RSS_FEED,
                                    author,
                                    null,
                                    feed.getTitle() != null ? feed.getTitle().trim() : null,
                                    null,
                                    publishedAt,
                                    Map.of("feedUrl", feedUrl)
                            ));
                            count++;
                        }
                    }
                    logger.info(String.format("RSS: Ingested %d entries from feed: %s", count, feedUrl));
                } else {
                    logger.warn(String.format("RSS: Failed to fetch feed %s (HTTP %d)", feedUrl, response.statusCode()));
                }

                Thread.sleep(800);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("RSS collection interrupted", e);
                break;
            } catch (Exception e) {
                logger.error("RSS error reading feed '" + feedUrl + "'", e);
            }
        }

        return results;
    }
}
