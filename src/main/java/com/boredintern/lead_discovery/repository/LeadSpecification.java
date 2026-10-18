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
            predicates.add(cb.isTrue(root.get("isIndianMarket")));

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

            if (filter.getPlatform() != null) {
                predicates.add(cb.equal(root.get("sourcePlatform"), filter.getPlatform()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
