package com.boredintern.lead_discovery.repository;

import com.boredintern.lead_discovery.model.CollectionRun;
import com.boredintern.lead_discovery.model.RunStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CollectionRunRepository extends JpaRepository<CollectionRun, Long> {
    List<CollectionRun> findTop50ByOrderByStartTimeDesc();
    List<CollectionRun> findByStatus(RunStatus status);
}
