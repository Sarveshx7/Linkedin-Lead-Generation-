package com.boredintern.lead_discovery.service;

import com.boredintern.lead_discovery.model.Lead;
import com.boredintern.lead_discovery.repository.LeadRepository;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Optional;

@Service
public class DeduplicationService {

    private final LeadRepository leadRepository;

    public DeduplicationService(LeadRepository leadRepository) {
        this.leadRepository = leadRepository;
    }

    public String canonicalizeUrl(String url) {
        if (url == null || url.isBlank()) {
            return "";
        }
        try {
            URI uri = URI.create(url.trim());
            String path = uri.getPath() != null ? uri.getPath().replaceAll("/+$", "") : "";
            // Strip UTM query tracking params
            String query = uri.getQuery();
            String cleanQuery = "";
            if (query != null && !query.isBlank()) {
                String[] params = query.split("&");
                StringBuilder qb = new StringBuilder();
                for (String p : params) {
                    if (!p.toLowerCase().startsWith("utm_") && !p.toLowerCase().startsWith("ref=")) {
                        if (!qb.isEmpty()) qb.append("&");
                        qb.append(p);
                    }
                }
                cleanQuery = qb.isEmpty() ? "" : "?" + qb;
            }
            return (uri.getScheme() != null ? uri.getScheme().toLowerCase() + "://" : "https://")
                    + (uri.getHost() != null ? uri.getHost().toLowerCase() : "")
                    + path + cleanQuery;
        } catch (Exception e) {
            return url.trim();
        }
    }

    public String computeFingerprint(String title, String content, String canonicalUrl) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String normalizedTitle = (title != null ? title : "").toLowerCase().replaceAll("[^a-z0-9]", "");
            // Use first 300 chars of normalized content to absorb slight formatting changes
            String normalizedContent = (content != null ? content : "").toLowerCase().replaceAll("[^a-z0-9]", "");
            if (normalizedContent.length() > 300) {
                normalizedContent = normalizedContent.substring(0, 300);
            }

            String base = normalizedTitle + "||" + normalizedContent;
            // If title and content are very sparse, append canonical URL
            if (base.length() < 30 && canonicalUrl != null) {
                base += "||" + canonicalUrl.toLowerCase();
            }

            byte[] hash = digest.digest(base.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            return Integer.toHexString((title + content).hashCode());
        }
    }

    public boolean isDuplicate(String fingerprint) {
        return leadRepository.existsByFingerprint(fingerprint);
    }

    public Optional<Lead> findExisting(String fingerprint) {
        return leadRepository.findByFingerprint(fingerprint);
    }
}
