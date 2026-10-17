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
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class LinkedInCollector implements LeadSourceCollector {

    private static final int MAX_RESULTS_PER_PROVIDER = 10;
    private static final int MAX_KEYWORDS = 12;
    private static final int MAX_EXCLUDED_KEYWORDS = 6;
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(20);
    private static final Pattern INTENT = Pattern.compile(
            "(?i)\\b(looking for|seeking|need(?:ing)? (?:a|an|the)?|recommend(?:ation)?|anyone know|" +
                    "hiring|hire|sponsor(?:ship|ed)?|brand partnership|paid partnership|creator partnership|" +
                    "collab(?:oration|orate|s)?|looking to partner|partner(?:ship)? with|" +
                    "creator(?:s)? (?:wanted|needed)|influencer(?:s)? (?:wanted|needed)|call for creators|" +
                    "request for proposal|\\brfp\\b|pitch invited|looking to work with|" +
                    "open to proposals|vendor search)\\b"
    );
    private static final Set<String> NON_ORGANIZATION_VALUES = Set.of(
            "linkedin", "linkedin corporation", "linkedin member", "unknown", ""
    );

    @Value("${lead.collector.linkedin.tavily-api-key:}")
    private String tavilyApiKey;

    @Value("${lead.collector.linkedin.exa-api-key:}")
    private String exaApiKey;

    @Value("${lead.collector.linkedin.serper-api-key:}")
    private String serperApiKey;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public LinkedInCollector(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();
    }

    @Override
    public SourcePlatform getPlatform() {
        return SourcePlatform.LINKEDIN;
    }

    @Override
    public boolean isAvailable() {
        return !configuredProviders().isEmpty();
    }

    /** Safe provider setup details for the settings page; API keys are never returned. */
    public List<Map<String, Object>> getProviderStatuses() {
        List<Map<String, Object>> statuses = new ArrayList<>();
        statuses.add(providerStatus("Tavily", hasText(tavilyApiKey), "TAVILY_API_KEY",
                "1,000 free API credits per month; no card required",
                "https://www.tavily.com/", "https://docs.tavily.com/documentation/api-reference/endpoint/search"));
        statuses.add(providerStatus("Exa", hasText(exaApiKey), "EXA_API_KEY",
                "$10 free credits each month plus a $10 signup bonus; no payment method required",
                "https://exa.ai/", "https://exa.ai/docs/reference/search"));
        statuses.add(providerStatus("Serper", hasText(serperApiKey), "SERPER_API_KEY",
                "2,500 free queries on signup (allowance shown by provider)",
                "https://serper.dev/", "https://serper.dev/"));
        return statuses;
    }

    @Override
    public List<RawLeadPayload> collect(Campaign campaign, RunLogger logger) {
        List<Provider> providers = configuredProviders();
        if (providers.isEmpty()) {
            logger.warn("LinkedIn search is not configured. Add one or more backend keys: TAVILY_API_KEY, EXA_API_KEY, SERPER_API_KEY.");
            return List.of();
        }

        String campaignQuery = buildQuery(campaign);
        logger.info("LinkedIn search: sending the same campaign query to " + providers.size()
                + " configured provider(s): " + providers.stream().map(Provider::name).collect(Collectors.joining(", ")) + ".");

        List<CompletableFuture<ProviderResponse>> requests = providers.stream()
                .map(provider -> CompletableFuture.supplyAsync(() -> searchSafely(provider, campaignQuery)))
                .toList();
        List<Candidate> candidates = new ArrayList<>();
        int successfulProviders = 0;
        for (CompletableFuture<ProviderResponse> request : requests) {
            ProviderResponse response;
            try {
                response = request.join();
            } catch (Exception e) {
                logger.warn("A LinkedIn search provider failed: " + safeMessage(e));
                continue;
            }
            if (response.error() != null) {
                logger.warn(response.name() + " search failed: " + response.error());
                continue;
            }
            successfulProviders++;
            logger.info(response.name() + " returned " + response.candidates().size() + " indexed result(s).");
            candidates.addAll(response.candidates());
        }
        if (successfulProviders == 0) {
            logger.warn("No configured LinkedIn search provider completed successfully.");
            return List.of();
        }

        Map<String, MatchedPost> byUrl = new LinkedHashMap<>();
        for (Candidate candidate : candidates) {
            if (!isExactLinkedInPostUrl(candidate.url())) {
                continue;
            }
            String canonicalUrl = canonicalPostUrl(candidate.url());
            if (canonicalUrl == null) {
                continue;
            }
            byUrl.computeIfAbsent(canonicalUrl, MatchedPost::new).add(candidate);
        }

        List<String> campaignTerms = campaignTerms(campaign);
        List<RawLeadPayload> leads = new ArrayList<>();
        int discarded = 0;
        for (MatchedPost post : byUrl.values()) {
            String searchable = post.searchableText().toLowerCase(Locale.ROOT);
            if (containsExcludedKeyword(searchable, campaign)
                    || !INTENT.matcher(searchable).find()
                    || campaignTerms.stream().noneMatch(term -> searchable.contains(term.toLowerCase(Locale.ROOT)))) {
                discarded++;
                continue;
            }

            Candidate best = post.bestCandidate();
            List<String> sources = new ArrayList<>(post.providers);
            Map<String, String> metadata = new LinkedHashMap<>();
            metadata.put("searchQuery", campaignQuery);
            metadata.put("searchProviders", String.join(", ", sources));
            metadata.put("providerMatchCount", Integer.toString(sources.size()));
            metadata.put("providerCount", Integer.toString(successfulProviders));
            metadata.put("providerAgreement", sources.size() > 1 ? "cross-checked" : "single-provider");
            metadata.put("campaignKeywords", campaignTerms.stream().filter(term -> searchable.contains(term.toLowerCase(Locale.ROOT)))
                    .collect(Collectors.joining(", ")));

            leads.add(new RawLeadPayload(
                    firstUseful(post.bestTitle(), firstUseful(best.title(), best.content())),
                    best.content().isBlank() ? firstUseful(post.bestTitle(), best.title()) : best.content(),
                    post.url,
                    SourcePlatform.LINKEDIN,
                    post.author,
                    post.authorProfileUrl,
                    post.company,
                    null,
                    post.publishedAt,
                    metadata
            ));
        }
        logger.info("Provider comparison merged " + byUrl.size() + " exact LinkedIn posts; filtered out " + discarded
                + " weak, excluded, or non-campaign matches; " + leads.size() + " lead(s) passed qualification.");
        return leads;
    }

    private ProviderResponse searchSafely(Provider provider, String query) {
        try {
            return new ProviderResponse(provider.name(), search(provider, query), null);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new ProviderResponse(provider.name(), List.of(), "request interrupted");
        } catch (Exception e) {
            return new ProviderResponse(provider.name(), List.of(), safeMessage(e));
        }
    }

    private List<Candidate> search(Provider provider, String query) throws Exception {
        HttpRequest request = switch (provider.name()) {
            case "Tavily" -> jsonPost("https://api.tavily.com/search", provider.apiKey(), "Authorization", "Bearer ",
                    Map.of("query", query, "search_depth", "basic", "max_results", MAX_RESULTS_PER_PROVIDER,
                            "include_domains", List.of("linkedin.com"), "include_answer", false));
            case "Exa" -> jsonPost("https://api.exa.ai/search", provider.apiKey(), "x-api-key", "",
                    Map.of("query", query, "type", "auto", "numResults", MAX_RESULTS_PER_PROVIDER,
                            "includeDomains", List.of("linkedin.com"),
                            "contents", Map.of("highlights", true)));
            case "Serper" -> jsonPost("https://google.serper.dev/search", provider.apiKey(), "X-API-KEY", "",
                    Map.of("q", query, "num", MAX_RESULTS_PER_PROVIDER, "gl", "in", "hl", "en"));
            default -> throw new IllegalArgumentException("Unknown search provider");
        };

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("HTTP " + response.statusCode());
        }
        JsonNode root = objectMapper.readTree(response.body());
        return switch (provider.name()) {
            case "Tavily" -> parseTavily(root);
            case "Exa" -> parseExa(root);
            case "Serper" -> parseSerper(root);
            default -> List.of();
        };
    }

    private HttpRequest jsonPost(String url, String key, String header, String prefix, Object body) throws Exception {
        return HttpRequest.newBuilder(URI.create(url))
                .timeout(REQUEST_TIMEOUT)
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .header(header, prefix + key.trim())
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                .build();
    }

    private List<Candidate> parseTavily(JsonNode root) {
        List<Candidate> parsed = new ArrayList<>();
        for (JsonNode item : root.path("results")) {
            String title = text(item, "title");
            String content = text(item, "content");
            parsed.add(candidate("Tavily", text(item, "url"), title, content, null,
                    null, null, parsePublishedAt(text(item, "published_date"))));
        }
        return parsed;
    }

    private List<Candidate> parseExa(JsonNode root) {
        List<Candidate> parsed = new ArrayList<>();
        for (JsonNode item : root.path("results")) {
            String content = text(item, "text");
            if (content.isBlank() && item.path("highlights").isArray()) {
                List<String> highlights = new ArrayList<>();
                item.path("highlights").forEach(value -> highlights.add(value.asText("")));
                content = String.join(" ", highlights);
            }
            parsed.add(candidate("Exa", text(item, "url"), text(item, "title"), content,
                    text(item, "author"), null, null, parsePublishedAt(text(item, "publishedDate"))));
        }
        return parsed;
    }

    private List<Candidate> parseSerper(JsonNode root) {
        List<Candidate> parsed = new ArrayList<>();
        for (JsonNode item : root.path("organic")) {
            parsed.add(candidate("Serper", text(item, "link"), text(item, "title"), text(item, "snippet"),
                    null, null, null, parsePublishedAt(text(item, "date"))));
        }
        return parsed;
    }

    private Candidate candidate(String provider, String url, String title, String content,
                                String author, String authorProfileUrl, String company, Instant publishedAt) {
        String cleanedTitle = title == null ? "" : title.trim();
        String cleanedContent = content == null ? "" : content.trim();
        // Keep names only when the provider supplies a structured author field.
        // Guessing a person from a search-result title can mislabel a company or topic as the author.
        String usefulAuthor = usefulPerson(author);
        return new Candidate(provider, url == null ? "" : url.trim(), cleanedTitle, cleanedContent,
                usefulAuthor, linkedinProfileUrl(authorProfileUrl), usefulOrganization(company), publishedAt);
    }

    private String buildQuery(Campaign campaign) {
        List<String> included = campaignTerms(campaign).stream().limit(MAX_KEYWORDS)
                .map(LinkedInCollector::quote).toList();
        StringBuilder query = new StringBuilder(
                "(site:linkedin.com/posts OR site:linkedin.com/feed/update) ("
        ).append(String.join(" OR ", included)).append(')');
        if (campaign != null && hasText(campaign.getExcludedKeywords())) {
            List<String> excluded = splitTerms(campaign.getExcludedKeywords()).stream()
                    .limit(MAX_EXCLUDED_KEYWORDS).map(term -> "-" + quote(term)).toList();
            if (!excluded.isEmpty()) {
                query.append(' ').append(String.join(" ", excluded));
            }
        }
        return query.toString();
    }

    private List<String> campaignTerms(Campaign campaign) {
        LinkedHashSet<String> terms = new LinkedHashSet<>();
        addCategoryDefaults(terms, campaign);
        if (campaign != null) {
            addTerms(terms, campaign.getKeywords());
        }
        if (terms.isEmpty()) {
            terms.addAll(List.of("content creator", "influencer", "brand collab", "creator partnership",
                    "looking for an advertising agency", "creative agency RFP"));
        }
        return terms.stream().limit(MAX_KEYWORDS).toList();
    }

    private void addCategoryDefaults(Set<String> terms, Campaign campaign) {
        if (campaign == null || campaign.getName() == null) return;
        String name = campaign.getName().toLowerCase(Locale.ROOT);
        if (name.contains("influencer") || name.contains("creator") || name.contains("collab")
                || name.contains("social media") || name.contains("ugc") || name.contains("ambassador")) {
            terms.addAll(List.of("looking for a content creator", "looking for content creators",
                    "looking for an influencer", "looking for influencers", "brand collab",
                    "creator partnership", "UGC creator", "social media creator"));
        } else if (name.contains("advertising") || name.contains("creative") || name.contains("marketing")) {
            terms.addAll(List.of("looking for an advertising agency", "looking for a creative agency",
                    "social media advertising", "advertising campaign", "brand campaign", "creative agency RFP"));
        } else if (name.contains("production")) {
            terms.addAll(List.of("looking for a production house", "video production team", "commercial shoot",
                    "brand video production", "product shoot", "production partner"));
        } else if (name.contains("brand film")) {
            terms.addAll(List.of("brand film RFP", "looking for a brand film agency", "corporate film",
                    "product commercial", "brand video campaign", "TVC production"));
        }
    }

    private void addTerms(Set<String> output, String rawTerms) {
        if (!hasText(rawTerms)) return;
        for (String term : splitTerms(rawTerms)) {
            if (term.length() >= 3 && term.length() <= 120) output.add(term);
            if (output.size() >= MAX_KEYWORDS) return;
        }
    }

    private List<String> splitTerms(String rawTerms) {
        List<String> terms = new ArrayList<>();
        for (String value : rawTerms.split("[,;\\r\\n]+")) {
            String clean = value.trim();
            if (!clean.isEmpty()) terms.add(clean);
        }
        return terms;
    }

    private boolean containsExcludedKeyword(String searchableText, Campaign campaign) {
        if (campaign == null || !hasText(campaign.getExcludedKeywords())) return false;
        return splitTerms(campaign.getExcludedKeywords()).stream()
                .map(term -> term.toLowerCase(Locale.ROOT)).anyMatch(searchableText::contains);
    }

    private String usefulPerson(String value) {
        if (!hasText(value)) return null;
        String person = value.trim();
        if (person.startsWith("http://") || person.startsWith("https://")
                || person.toLowerCase(Locale.ROOT).contains("linkedin")) return null;
        return person;
    }

    private String usefulOrganization(String value) {
        if (!hasText(value) || NON_ORGANIZATION_VALUES.contains(value.trim().toLowerCase(Locale.ROOT))) return null;
        return value.trim();
    }

    private String linkedinProfileUrl(String value) {
        if (!hasText(value)) return null;
        try {
            URI uri = URI.create(value.trim());
            String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
            String path = uri.getPath() == null ? "" : uri.getPath().toLowerCase(Locale.ROOT);
            if ("https".equalsIgnoreCase(uri.getScheme()) && isLinkedInHost(host) && path.startsWith("/in/")) {
                return uri.toString();
            }
        } catch (IllegalArgumentException ignored) { }
        return null;
    }

    private Instant parsePublishedAt(String value) {
        if (!hasText(value)) return null;
        try { return Instant.parse(value.trim()); } catch (DateTimeParseException ignored) { }
        try { return OffsetDateTime.parse(value.trim()).toInstant(); } catch (DateTimeParseException ignored) { }
        try { return LocalDate.parse(value.trim()).atStartOfDay().toInstant(java.time.ZoneOffset.UTC); }
        catch (DateTimeParseException ignored) { return null; }
    }

    private boolean isExactLinkedInPostUrl(String value) {
        try {
            URI uri = URI.create(value);
            String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
            String path = uri.getPath() == null ? "" : uri.getPath().toLowerCase(Locale.ROOT);
            if (!"https".equalsIgnoreCase(uri.getScheme()) || !isLinkedInHost(host)) return false;
            return path.matches("(?i)^/(?:[a-z]{2}/)?posts/[^/]*[-_](?:activity|share|ugcpost)-\\d{8,}(?:-[a-z0-9_-]+)?/?$")
                    || path.matches("(?i)^/(?:[a-z]{2}/)?feed/update/urn:li:(?:activity|share|ugcpost):\\d+/?$");
        } catch (IllegalArgumentException e) { return false; }
    }

    private String canonicalPostUrl(String value) {
        try {
            URI uri = URI.create(value);
            String path = uri.getPath() == null ? "" : uri.getPath().replaceAll("/+$", "");
            if (path.isBlank()) return null;
            return "https://www.linkedin.com" + path;
        } catch (IllegalArgumentException e) { return null; }
    }

    private List<Provider> configuredProviders() {
        List<Provider> providers = new ArrayList<>();
        if (hasText(tavilyApiKey)) providers.add(new Provider("Tavily", tavilyApiKey));
        if (hasText(exaApiKey)) providers.add(new Provider("Exa", exaApiKey));
        if (hasText(serperApiKey)) providers.add(new Provider("Serper", serperApiKey));
        return providers;
    }

    private Map<String, Object> providerStatus(String name, boolean configured, String envVar,
                                               String allowance, String signupUrl, String docsUrl) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("name", name);
        value.put("configured", configured);
        value.put("envVar", envVar);
        value.put("freeAllowance", allowance);
        value.put("signupUrl", signupUrl);
        value.put("docsUrl", docsUrl);
        return value;
    }

    private String text(JsonNode node, String field) {
        return node.path(field).asText("").trim();
    }

    private String firstUseful(String first, String second) {
        return hasText(first) ? first.trim() : (hasText(second) ? second.trim() : null);
    }

    private boolean isLinkedInHost(String host) {
        return host.equals("linkedin.com") || host.endsWith(".linkedin.com");
    }

    private static String quote(String value) {
        return "\"" + value.replace("\"", "").trim() + "\"";
    }

    private static String safeMessage(Throwable error) {
        Throwable cause = error.getCause() == null ? error : error.getCause();
        String message = cause.getMessage();
        return hasText(message) ? message : cause.getClass().getSimpleName();
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private record Provider(String name, String apiKey) { }
    private record ProviderResponse(String name, List<Candidate> candidates, String error) { }
    private record Candidate(String provider, String url, String title, String content, String author,
                             String authorProfileUrl, String company, Instant publishedAt) { }

    private static final class MatchedPost {
        private final String url;
        private final List<Candidate> items = new ArrayList<>();
        private final Set<String> providers = new LinkedHashSet<>();
        private String author;
        private String authorProfileUrl;
        private String company;
        private Instant publishedAt;

        private MatchedPost(String url) { this.url = url; }

        private void add(Candidate candidate) {
            items.add(candidate);
            providers.add(candidate.provider());
            if (!hasText(author)) author = candidate.author();
            if (!hasText(authorProfileUrl)) authorProfileUrl = candidate.authorProfileUrl();
            if (!hasText(company)) company = candidate.company();
            if (publishedAt == null) publishedAt = candidate.publishedAt();
        }

        private String searchableText() {
            return items.stream().map(item -> item.title() + " " + item.content()).collect(Collectors.joining(" "));
        }

        private Candidate bestCandidate() {
            return items.stream().max(Comparator.comparingInt(item -> item.content().length())).orElseThrow();
        }

        private String bestTitle() {
            return items.stream().map(Candidate::title).filter(LinkedInCollector::hasText)
                    .max(Comparator.comparingInt(String::length)).orElse("");
        }
    }
}
