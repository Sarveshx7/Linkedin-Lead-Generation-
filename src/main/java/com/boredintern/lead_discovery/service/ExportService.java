package com.boredintern.lead_discovery.service;

import com.boredintern.lead_discovery.model.Lead;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

@Service
public class ExportService {

    private static final String[] HEADERS = {
            "ID", "Title", "Platform", "Source URL", "Author", "Company", "Location",
            "Published Date", "Discovered Date", "Service Category", "Intent Level",
            "Indian Market", "Relevance Score", "Review Status", "Contact Info", "User Notes", "Content"
    };

    public void writeLeadsToCsv(List<Lead> leads, Writer writer) throws IOException {
        CSVFormat format = CSVFormat.Builder.create(CSVFormat.DEFAULT)
                .setHeader(HEADERS)
                .setRecordSeparator("\r\n")
                .build();

        try (CSVPrinter printer = new CSVPrinter(writer, format)) {
            for (Lead lead : leads) {
                printer.printRecord(
                        lead.getId(),
                        lead.getTitle(),
                        lead.getSourcePlatform() != null ? lead.getSourcePlatform().name() : "",
                        lead.getSourceUrl(),
                        lead.getAuthor(),
                        lead.getCompany(),
                        lead.getLocation(),
                        lead.getPublishedAt() != null ? lead.getPublishedAt().toString() : "",
                        lead.getDiscoveredAt() != null ? lead.getDiscoveredAt().toString() : "",
                        lead.getServiceCategory() != null ? lead.getServiceCategory().name() : "",
                        lead.getIntentLevel() != null ? lead.getIntentLevel().name() : "",
                        Boolean.TRUE.equals(lead.getIsIndianMarket()) ? "Yes" : "No",
                        lead.getRelevanceScore(),
                        lead.getReviewStatus() != null ? lead.getReviewStatus().name() : "",
                        lead.getContactInfo(),
                        lead.getUserNotes(),
                        lead.getContent()
                );
            }
            printer.flush();
        }
    }
}
