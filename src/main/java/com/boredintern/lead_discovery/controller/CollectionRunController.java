package com.boredintern.lead_discovery.controller;

import com.boredintern.lead_discovery.model.CollectionRun;
import com.boredintern.lead_discovery.repository.CollectionRunRepository;
import com.boredintern.lead_discovery.service.LeadCollectionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/collection")
@CrossOrigin(origins = "*")
public class CollectionRunController {

    private final CollectionRunRepository collectionRunRepository;
    private final LeadCollectionService collectionService;

    public CollectionRunController(CollectionRunRepository collectionRunRepository, LeadCollectionService collectionService) {
        this.collectionRunRepository = collectionRunRepository;
        this.collectionService = collectionService;
    }

    @GetMapping("/runs")
    public ResponseEntity<List<CollectionRun>> getRuns() {
        return ResponseEntity.ok(collectionRunRepository.findTop50ByOrderByStartTimeDesc());
    }

    @GetMapping("/runs/{id}")
    public ResponseEntity<CollectionRun> getRunById(@PathVariable Long id) {
        return collectionRunRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/run")
    public ResponseEntity<CollectionRun> triggerGlobalRun(@RequestParam(required = false) Long campaignId) {
        CollectionRun run = collectionService.executeCollectionSync(campaignId);
        return ResponseEntity.ok(run);
    }
}
