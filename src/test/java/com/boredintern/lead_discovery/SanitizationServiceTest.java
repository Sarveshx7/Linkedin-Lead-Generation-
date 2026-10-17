package com.boredintern.lead_discovery;

import com.boredintern.lead_discovery.service.SanitizationService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SanitizationServiceTest {

    private final SanitizationService sanitizationService = new SanitizationService();

    @Test
    void testSanitizeHtmlStripsTagsAndDecodesEntities() {
        String html = "<p>Looking for a <b>React</b> &amp; <i>Node.js</i> developer.<br/>Urgent requirement!</p>";
        String result = sanitizationService.sanitizeHtml(html);
        assertTrue(result.contains("Looking for a React & Node.js developer."));
        assertTrue(result.contains("Urgent requirement!"));
        assertFalse(result.contains("<p>"));
        assertFalse(result.contains("&amp;"));
    }

    @Test
    void testExtractContactInfoFindsEmailAndPhone() {
        String text = "Contact us at hiring@techagency.in or call +919876543210 for details.";
        String contacts = sanitizationService.extractContactInfo(text);
        assertNotNull(contacts);
        assertTrue(contacts.contains("hiring@techagency.in"));
        assertTrue(contacts.contains("9876543210"));
    }
}
