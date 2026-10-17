package com.boredintern.lead_discovery.controller;

import com.boredintern.lead_discovery.collector.LeadSourceCollector;
import com.boredintern.lead_discovery.collector.LinkedInCollector;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.net.URI;
import java.util.*;

@RestController
@RequestMapping("/api/system")
@CrossOrigin(origins = "*")
public class SystemController {

    private final DataSource dataSource;
    private final List<LeadSourceCollector> collectors;

    @Value("${spring.datasource.url:}")
    private String datasourceUrl;

    public SystemController(DataSource dataSource, List<LeadSourceCollector> collectors) {
        this.dataSource = dataSource;
        this.collectors = collectors;
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getSystemStatus() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("status", "UP");
        status.put("timestamp", System.currentTimeMillis());

        // Database status
        Map<String, Object> dbInfo = new LinkedHashMap<>();
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            String dbProduct = meta.getDatabaseProductName();
            String dbVersion = meta.getDatabaseProductVersion();
            dbInfo.put("connected", true);
            dbInfo.put("databaseProduct", dbProduct);
            dbInfo.put("databaseVersion", dbVersion);
            boolean isSupabase = dbProduct != null
                    && dbProduct.toLowerCase(Locale.ROOT).contains("postgres")
                    && isSupabaseJdbcUrl(datasourceUrl);
            dbInfo.put("isSupabase", isSupabase);
            dbInfo.put("mode", isSupabase ? "Supabase Cloud PostgreSQL" : dbProduct);
        } catch (Exception e) {
            dbInfo.put("connected", false);
            dbInfo.put("error", e.getMessage());
        }
        status.put("database", dbInfo);

        // Collectors status
        List<Map<String, Object>> collectorList = new ArrayList<>();
        for (LeadSourceCollector c : collectors) {
            Map<String, Object> cInfo = new LinkedHashMap<>();
            cInfo.put("platform", c.getPlatform().name());
            cInfo.put("available", c.isAvailable());
            cInfo.put("configured", c.isAvailable());
            if (c.getPlatform() == com.boredintern.lead_discovery.model.SourcePlatform.LINKEDIN) {
                cInfo.put("accessMethod", "PUBLIC_SEARCH_INDEX");
                if (c instanceof LinkedInCollector linkedInCollector) {
                    cInfo.put("searchProviders", linkedInCollector.getProviderStatuses());
                }
            }
            collectorList.add(cInfo);
        }
        status.put("collectors", collectorList);

        return ResponseEntity.ok(status);
    }

    private boolean isSupabaseJdbcUrl(String jdbcUrl) {
        if (jdbcUrl == null || !jdbcUrl.startsWith("jdbc:")) return false;
        try {
            String url = jdbcUrl.substring("jdbc:".length());
            String host = URI.create(url).getHost();
            if (host == null) return false;
            String normalizedHost = host.toLowerCase(Locale.ROOT);
            return normalizedHost.endsWith(".supabase.co") || normalizedHost.endsWith(".supabase.com");
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }
}
