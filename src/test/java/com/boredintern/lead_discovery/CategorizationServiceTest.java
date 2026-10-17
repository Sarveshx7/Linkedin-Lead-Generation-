package com.boredintern.lead_discovery;

import com.boredintern.lead_discovery.model.IntentLevel;
import com.boredintern.lead_discovery.model.ServiceCategory;
import com.boredintern.lead_discovery.service.CategorizationService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CategorizationServiceTest {

    private final CategorizationService categorizationService = new CategorizationService();

    @Test
    void testInfluencerMarketingCategoryAndHighIntent() {
        String title = "Looking for an influencer marketing agency for our upcoming campaign";
        String content = "We are searching for an influencer agency with creators in Mumbai for our D2C brand launch. Budget 25 Lakhs INR.";
        CategorizationService.CategorizationResult result = categorizationService.analyze(title, content, "Shreya", "https://linkedin.com/posts/123", "Mumbai");

        assertTrue(result.isIndianMarket());
        assertEquals(ServiceCategory.INFLUENCER_MARKETING, result.category());
        assertEquals(IntentLevel.HIGH, result.intentLevel());
        assertTrue(result.relevanceScore() >= 80);
    }

    @Test
    void testProductionHouseAndBrandFilmCategory() {
        String title = "Can anyone recommend a production house for our upcoming product shoot?";
        String content = "We need a commercial shoot for our product launch in Bangalore.";
        CategorizationService.CategorizationResult result = categorizationService.analyze(title, content, "Vikram", "https://linkedin.com/posts/456", "Bangalore");

        assertEquals(ServiceCategory.PRODUCTION_HOUSE, result.category());
        assertEquals(IntentLevel.HIGH, result.intentLevel());

        String filmTitle = "We need a creative team to produce a brand film for our new product launch";
        String filmContent = "Seeking a director and creative studio for our 90-second brand video and corporate film.";
        CategorizationService.CategorizationResult filmResult = categorizationService.analyze(filmTitle, filmContent, "Kavita", "https://linkedin.com/posts/789", "Mumbai");

        assertEquals(ServiceCategory.BRAND_FILM, filmResult.category());
        assertEquals(IntentLevel.HIGH, filmResult.intentLevel());
    }
}
