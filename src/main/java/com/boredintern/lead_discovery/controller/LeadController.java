package com.boredintern.lead_discovery.controller;

import com.boredintern.lead_discovery.dto.LeadFilterRequest;
import com.boredintern.lead_discovery.dto.LeadStatusUpdateRequest;
import com.boredintern.lead_discovery.model.Lead;
import com.boredintern.lead_discovery.repository.LeadRepository;
import com.boredintern.lead_discovery.repository.LeadSpecification;
import com.boredintern.lead_discovery.service.ExportService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/leads")
@CrossOrigin(origins = "*")
public class LeadController {

    private final LeadRepository leadRepository;
    private final ExportService exportService;
    private final com.boredintern.lead_discovery.service.LeadCollectionService collectionService;

    public LeadController(
            LeadRepository leadRepository,
            ExportService exportService,
            com.boredintern.lead_discovery.service.LeadCollectionService collectionService
    ) {
        this.leadRepository = leadRepository;
        this.exportService = exportService;
        this.collectionService = collectionService;
    }

    @PostMapping("/reset-to-linkedin")
    public ResponseEntity<String> resetToLinkedIn() {
        collectionService.purgeAndCollectLinkedInOnly();
        return ResponseEntity.ok("Successfully cleared old leads and ingested target LinkedIn leads!");
    }

    @GetMapping
    public ResponseEntity<Page<Lead>> getLeads(LeadFilterRequest filter) {
        Sort.Direction direction = "asc".equalsIgnoreCase(filter.getSortDir()) ? Sort.Direction.ASC : Sort.Direction.DESC;
        String sortProperty = filter.getSortBy() != null ? filter.getSortBy() : "discoveredAt";

        Pageable pageable = PageRequest.of(Math.max(0, filter.getPage()), Math.max(1, filter.getSize()), buildSort(sortProperty, direction));
        Page<Lead> leads = leadRepository.findAll(LeadSpecification.withFilters(filter), pageable);
        return ResponseEntity.ok(leads);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Lead> getLeadById(@PathVariable Long id) {
        return leadRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLead(@PathVariable Long id) {
        return leadRepository.findById(id).map(lead -> {
            leadRepository.delete(lead);
            return ResponseEntity.noContent().<Void>build();
        }).orElseGet(() -> ResponseEntity.notFound().<Void>build());
    }

    @DeleteMapping("/all")
    @Transactional
    public ResponseEntity<java.util.Map<String, Long>> deleteAllLeads() {
        long deletedCount = leadRepository.count();
        leadRepository.deleteAllInBatch();
        return ResponseEntity.ok(java.util.Map.of("deletedCount", deletedCount));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Lead> updateStatus(
            @PathVariable Long id,
            @RequestBody LeadStatusUpdateRequest request
    ) {
        return leadRepository.findById(id).map(lead -> {
            if (request.getStatus() != null) {
                lead.setReviewStatus(request.getStatus());
            }
            if (request.getUserNotes() != null) {
                lead.setUserNotes(request.getUserNotes());
            }
            return ResponseEntity.ok(leadRepository.save(lead));
        }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/export")
    public void exportCsv(LeadFilterRequest filter, HttpServletResponse response) throws IOException {
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=leads_export.csv");

        Sort.Direction direction = "asc".equalsIgnoreCase(filter.getSortDir()) ? Sort.Direction.ASC : Sort.Direction.DESC;
        String sortProperty = filter.getSortBy() != null ? filter.getSortBy() : "discoveredAt";

        List<Lead> leads = leadRepository.findAll(
                LeadSpecification.withFilters(filter),
                buildSort(sortProperty, direction)
        );

        exportService.writeLeadsToCsv(leads, response.getWriter());
    }

    private Sort buildSort(String sortProperty, Sort.Direction direction) {
        if ("intentLevel".equals(sortProperty)) {
            // Intent text sorts alphabetically, so use the mapped priority formula instead.
            return Sort.by(direction, "intentPriority")
                    .and(Sort.by(Sort.Direction.DESC, "discoveredAt"));
        }
        return Sort.by(direction, sortProperty);
    }
}
