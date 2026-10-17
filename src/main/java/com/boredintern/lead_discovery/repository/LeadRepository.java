package com.boredintern.lead_discovery.repository;

import com.boredintern.lead_discovery.model.IntentLevel;
import com.boredintern.lead_discovery.model.Lead;
import com.boredintern.lead_discovery.model.ReviewStatus;
import com.boredintern.lead_discovery.model.ServiceCategory;
import com.boredintern.lead_discovery.model.SourcePlatform;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LeadRepository extends JpaRepository<Lead, Long>, JpaSpecificationExecutor<Lead> {

    Optional<Lead> findByFingerprint(String fingerprint);

    boolean existsByFingerprint(String fingerprint);

    long countByReviewStatus(ReviewStatus reviewStatus);

    long countByIntentLevel(IntentLevel intentLevel);

    long countByIsIndianMarket(Boolean isIndianMarket);

    long countByServiceCategory(ServiceCategory serviceCategory);

    long countBySourcePlatform(SourcePlatform sourcePlatform);

    @Query("SELECT l.serviceCategory, COUNT(l) FROM Lead l GROUP BY l.serviceCategory")
    List<Object[]> countGroupByServiceCategory();

    @Query("SELECT l.intentLevel, COUNT(l) FROM Lead l GROUP BY l.intentLevel")
    List<Object[]> countGroupByIntentLevel();

    @Query("SELECT l.sourcePlatform, COUNT(l) FROM Lead l GROUP BY l.sourcePlatform")
    List<Object[]> countGroupBySourcePlatform();

    List<Lead> findTop25ByOrderByDiscoveredAtDesc();

    List<Lead> findTop25ByIntentLevelAndSourcePlatformOrderByDiscoveredAtDesc(
            IntentLevel intentLevel,
            SourcePlatform sourcePlatform
    );
}
