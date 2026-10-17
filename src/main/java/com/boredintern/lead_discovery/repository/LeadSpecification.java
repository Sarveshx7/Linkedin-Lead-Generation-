package com.boredintern.lead_discovery.repository;

import com.boredintern.lead_discovery.dto.LeadFilterRequest;
import com.boredintern.lead_discovery.model.Lead;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class LeadSpecification {

    public static Specification<Lead> withFilters(LeadFilterRequest filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filter.getQuery() != null && !filter.getQuery().isBlank()) {
                String term = "%" + filter.getQuery().trim().toLowerCase() + "%";
                Predicate titleMatch = cb.like(cb.lower(root.get("title")), term);
                Predicate contentMatch = cb.like(cb.lower(root.get("content")), term);
                Predicate kwMatch = cb.like(cb.lower(root.get("detectedKeywords")), term);
                predicates.add(cb.or(titleMatch, contentMatch, kwMatch));
            }

            if (filter.getCategory() != null) {
                predicates.add(cb.equal(root.get("serviceCategory"), filter.getCategory()));
            }

            if (filter.getIntent() != null) {
                predicates.add(cb.equal(root.get("intentLevel"), filter.getIntent()));
            }

            if (filter.getStatus() != null) {
                predicates.add(cb.equal(root.get("reviewStatus"), filter.getStatus()));
            }

            if (filter.getIsIndian() != null) {
                predicates.add(cb.equal(root.get("isIndianMarket"), filter.getIsIndian()));
            }

            if (filter.getPlatform() != null) {
                predicates.add(cb.equal(root.get("sourcePlatform"), filter.getPlatform()));
            }

            if (filter.getTimeRange() != null && !filter.getTimeRange().isBlank()) {
                java.time.Instant now = java.time.Instant.now();
                java.time.Instant cutoff = null;
                if ("24h".equalsIgnoreCase(filter.getTimeRange())) {
                    cutoff = now.minus(java.time.Duration.ofHours(24));
                } else if ("week".equalsIgnoreCase(filter.getTimeRange()) || "7d".equalsIgnoreCase(filter.getTimeRange())) {
                    cutoff = now.minus(java.time.Duration.ofDays(7));
                } else if ("month".equalsIgnoreCase(filter.getTimeRange()) || "30d".equalsIgnoreCase(filter.getTimeRange())) {
                    cutoff = now.minus(java.time.Duration.ofDays(30));
                }
                if (cutoff != null) {
                    predicates.add(cb.greaterThanOrEqualTo(root.get("publishedAt"), cutoff));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
