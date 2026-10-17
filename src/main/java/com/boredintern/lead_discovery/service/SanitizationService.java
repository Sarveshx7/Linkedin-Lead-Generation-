package com.boredintern.lead_discovery.service;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class SanitizationService {

    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]+>");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");
    private static final Pattern PHONE_PATTERN = Pattern.compile("(?:\\+?91[\\s-]?)?[6-9]\\d{9}");
    private static final Pattern WHATSAPP_LINK_PATTERN = Pattern.compile("(?:https?:\\/\\/)?(?:wa\\.me|api\\.whatsapp\\.com)\\/[0-9]+");

    public String sanitizeHtml(String raw) {
        if (raw == null) {
            return "";
        }
        // Replace paragraph and break tags with newlines
        String withBreaks = raw.replaceAll("(?i)<br\\s*/?>", "\n")
                .replaceAll("(?i)</p>", "\n\n")
                .replaceAll("(?i)</li>", "\n");

        // Strip remaining HTML tags
        String stripped = HTML_TAG_PATTERN.matcher(withBreaks).replaceAll("");

        // Decode common HTML entities
        String decoded = stripped
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("&nbsp;", " ")
                .replace("&#x2F;", "/");

        // Normalize multiple spaces/newlines
        return decoded.replaceAll("[\\t ]+", " ").replaceAll("\\n{3,}", "\n\n").trim();
    }

    public String extractContactInfo(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }

        Set<String> contacts = new LinkedHashSet<>();

        // Match emails
        Matcher emailMatcher = EMAIL_PATTERN.matcher(text);
        while (emailMatcher.find()) {
            String email = emailMatcher.group().toLowerCase();
            // Exclude common image extensions or spam domains
            if (!email.endsWith(".png") && !email.endsWith(".jpg") && !email.endsWith(".jpeg") && !email.endsWith(".gif")) {
                contacts.add("Email: " + email);
            }
        }

        // Match WhatsApp links
        Matcher waMatcher = WHATSAPP_LINK_PATTERN.matcher(text);
        while (waMatcher.find()) {
            contacts.add("WhatsApp: " + waMatcher.group());
        }

        // Match Indian phone numbers
        Matcher phoneMatcher = PHONE_PATTERN.matcher(text);
        while (phoneMatcher.find()) {
            contacts.add("Phone: " + phoneMatcher.group());
        }

        if (contacts.isEmpty()) {
            return null;
        }
        return String.join(" | ", contacts);
    }
}
