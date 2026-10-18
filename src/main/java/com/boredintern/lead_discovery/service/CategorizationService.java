package com.boredintern.lead_discovery.service;

import com.boredintern.lead_discovery.model.IntentLevel;
import com.boredintern.lead_discovery.model.ServiceCategory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class CategorizationService {

    private static final Pattern INDIA_MARKET_SIGNAL = Pattern.compile(
            "\\b(?:india[- ]based|(?:mumbai|delhi|new delhi|ncr|gurgaon|gurugram|noida|bangalore|bengaluru|hyderabad|pune|chennai|kolkata|ahmedabad|jaipur|kochi|kerala|goa|chandigarh|indore|tamil nadu|karnataka|telangana|andhra pradesh)[- ]based|"
                    + "indian (?:market|audience|consumers?|brands?|customers?)|india (?:market|campaign|launch|audience)|"
                    + "(?:based|located|headquartered|launching|campaign|available|operating|shipping|targeting|serving)\\s+(?:in|across|throughout|for)\\s+(?:the\\s+)?india|"
                    + "(?:in|across|throughout|within|from|targeting|serving)\\s+(?:the\\s+)?india|"
                    + "(?:in|across|from|within)\\s+(?:mumbai|delhi|new delhi|ncr|gurgaon|gurugram|noida|bangalore|bengaluru|hyderabad|pune|chennai|kolkata|ahmedabad|jaipur|kochi|kerala|goa|chandigarh|indore)|"
                    + "(?:based|located|headquartered|campaign|shoot|launch|event|available|operating)\\s+in\\s+(?:mumbai|delhi|new delhi|ncr|gurgaon|gurugram|noida|bangalore|bengaluru|hyderabad|pune|chennai|kolkata|ahmedabad|jaipur|kochi|kerala|goa|chandigarh|indore|tamil nadu|karnataka|telangana|andhra pradesh))\\b",
            Pattern.CASE_INSENSITIVE
    );

    private static final Map<ServiceCategory, List<String>> CATEGORY_KEYWORDS = new EnumMap<>(ServiceCategory.class);

    static {
        // 1. Influencer Marketing Leads
        CATEGORY_KEYWORDS.put(ServiceCategory.INFLUENCER_MARKETING, List.of(
                "influencer marketing", "influencer agency", "influencer campaign", "influencers",
                "creator marketing", "creator agency", "influencer collaborations", "macro influencers",
                "micro influencers", "creator management", "brand ambassadors", "influencer outreach",
                "kol", "influencer roster", "influencer seeding", "content creators for our brand",
                "influencer partnerships", "creators for campaign", "influencer budget",
                "content creator", "content creators", "brand sponsor", "brand sponsorship",
                "creator collaboration", "creator collaborations", "creators wanted", "looking for creators",
                "brand collab", "brand collaboration", "paid partnership", "sponsored content",
                "creator partnership", "ugc creator", "user generated content", "social media creator",
                "brand ambassador wanted", "influencers wanted"
        ));

        // 2. Advertising Leads
        CATEGORY_KEYWORDS.put(ServiceCategory.ADVERTISING, List.of(
                "advertising agency", "ad agency", "advertising campaign", "creative agency",
                "performance marketing agency", "media buying agency", "advertising strategy",
                "digital advertising", "ad campaign", "promotional services", "360 marketing agency",
                "agency pitch", "pitch invited", "ooh advertising", "brand advertising", "marketing campaign",
                "marketing leads", "performance marketing"
        ));

        // 3. Production House Leads
        CATEGORY_KEYWORDS.put(ServiceCategory.PRODUCTION_HOUSE, List.of(
                "production house", "video production", "commercial shoot", "product shoot",
                "ad shoot", "ecommerce shoot", "photo shoot", "post production", "production crew",
                "videographer", "production company", "shoot in mumbai", "shoot in bangalore",
                "shoot in delhi", "film production", "studio shoot", "shoot team", "video crew"
        ));

        // 4. Brand Film Leads
        CATEGORY_KEYWORDS.put(ServiceCategory.BRAND_FILM, List.of(
                "brand film", "corporate film", "product commercial", "brand video", "tvc",
                "tv commercial", "manifesto film", "digital film", "founder story video",
                "brand storytelling", "storytelling campaign", "commercial film", "video commercial",
                "brand anthem", "brand story"
        ));
    }

    private static final List<String> HIGH_INTENT_PHRASES = List.of(
            "looking for an influencer marketing agency", "looking for influencer marketing",
            "brand sponsor looking for a content creator", "brand sponsor looking for content creators",
            "looking for content creators", "looking for content creator", "marketing leads rfp",
            "looking for an advertising agency", "searching for an agency", "searching for an ad agency",
            "can anyone recommend a production house", "recommend a production house", "looking for a production house",
            "need a creative team to produce a brand film", "produce a brand film", "need a brand film",
            "looking to hire an agency", "looking for creative agency", "agency pitch invited",
            "pitch invited", "rfp for creative", "rfp for advertising", "commercial shoot",
            "product shoot for our brand", "budget allocated for campaign", "shoot scheduled",
            "looking for creators", "looking for influencers for our", "invite agencies to pitch",
            "we are searching for an agency", "looking for a video production team",
            "seeking content creators", "seeking influencers", "brand collab", "creator collaboration",
            "looking to collaborate with creators", "looking for ugc creators", "creator needed",
            "creators needed", "brand ambassador wanted", "paid brand partnership"
    );

    private static final List<String> MEDIUM_INTENT_PHRASES = List.of(
            "recommendation for agency", "recommendations for production", "exploring influencer agencies",
            "evaluating advertising agencies", "seeking advice on brand film", "looking for suggestions",
            "who are the best influencer marketing agencies", "best production house in mumbai"
    );

    public record CategorizationResult(
            ServiceCategory category,
            IntentLevel intentLevel,
            String intentReasoning,
            boolean isIndianMarket,
            int relevanceScore,
            int completenessScore,
            String detectedKeywords
    ) {}

    public CategorizationResult analyze(String title, String content, String author, String sourceUrl, String locationHint) {
        String fullText = ((title != null ? title : "") + " " + (content != null ? content : "") + " " + (locationHint != null ? locationHint : "")).toLowerCase();

        // 1. Service Category Detection
        ServiceCategory detectedCategory = ServiceCategory.OTHER;
        int maxCatMatches = 0;
        List<String> matchedKeywords = new ArrayList<>();

        for (Map.Entry<ServiceCategory, List<String>> entry : CATEGORY_KEYWORDS.entrySet()) {
            int matches = 0;
            for (String kw : entry.getValue()) {
                if (fullText.contains(kw)) {
                    matches++;
                    if (!matchedKeywords.contains(kw) && matchedKeywords.size() < 8) {
                        matchedKeywords.add(kw);
                    }
                }
            }
            if (matches > maxCatMatches) {
                maxCatMatches = matches;
                detectedCategory = entry.getKey();
            }
        }

        // 2. Commercial Intent Level
        IntentLevel intentLevel = IntentLevel.LOW;
        String intentReasoning = "Informational industry post or discussion.";

        for (String phrase : HIGH_INTENT_PHRASES) {
            if (fullText.contains(phrase)) {
                intentLevel = IntentLevel.HIGH;
                intentReasoning = "Direct commercial requirement detected: '" + phrase + "'";
                break;
            }
        }

        if (intentLevel == IntentLevel.LOW) {
            for (String phrase : MEDIUM_INTENT_PHRASES) {
                if (fullText.contains(phrase)) {
                    intentLevel = IntentLevel.MEDIUM;
                    intentReasoning = "Exploratory agency/production inquiry: '" + phrase + "'";
                    break;
                }
            }
        }

        // If high keyword count in target services, default to at least MEDIUM
        if (intentLevel == IntentLevel.LOW && maxCatMatches >= 2) {
            intentLevel = IntentLevel.MEDIUM;
            intentReasoning = "High concentration of category-specific commercial keywords.";
        }

        // 3. Indian Market Detection
        boolean isIndian = hasIndiaMarketSignal(fullText);
        if (isIndian) {
            matchedKeywords.add("explicit India market/location phrase");
        }

        // 4. Relevance Score (0 - 100)
        int relevance = 40;
        if (intentLevel == IntentLevel.HIGH) relevance += 35;
        else if (intentLevel == IntentLevel.MEDIUM) relevance += 20;

        if (isIndian) relevance += 15;
        if (detectedCategory != ServiceCategory.OTHER) relevance += 10;
        relevance = Math.min(100, relevance);

        // 5. Completeness Score (0 - 100)
        int completeness = 0;
        if (title != null && !title.isBlank()) completeness += 20;
        if (content != null && content.length() > 60) completeness += 25;
        if (author != null && !author.isBlank()) completeness += 15;
        if (sourceUrl != null && !sourceUrl.isBlank()) completeness += 20;
        if (isIndian || (locationHint != null && !locationHint.isBlank())) completeness += 20;
        completeness = Math.min(100, Math.max(25, completeness));

        return new CategorizationResult(
                detectedCategory,
                intentLevel,
                intentReasoning,
                isIndian,
                relevance,
                completeness,
                String.join(", ", matchedKeywords)
        );
    }

    private boolean isNegatedIndiaSignal(String text, int matchStart) {
        String context = text.substring(Math.max(0, matchStart - 24), matchStart);
        return Pattern.compile("\\b(?:not|outside|excluding|except|non[- ]?)\\s*$", Pattern.CASE_INSENSITIVE)
                .matcher(context).find();
    }

    private boolean hasIndiaMarketSignal(String text) {
        Matcher matcher = INDIA_MARKET_SIGNAL.matcher(text);
        while (matcher.find()) {
            if (!isNegatedIndiaSignal(text, matcher.start())) return true;
        }
        return false;
    }
}
