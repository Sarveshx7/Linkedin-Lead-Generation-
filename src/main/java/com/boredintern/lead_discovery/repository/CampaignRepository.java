package com.boredintern.lead_discovery.repository;

import com.boredintern.lead_discovery.model.Campaign;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CampaignRepository extends JpaRepository<Campaign, Long> {
    List<Campaign> findByIsActiveTrue();
}
