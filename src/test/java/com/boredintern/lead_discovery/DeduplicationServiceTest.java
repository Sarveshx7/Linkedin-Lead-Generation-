package com.boredintern.lead_discovery;

import com.boredintern.lead_discovery.repository.LeadRepository;
import com.boredintern.lead_discovery.service.DeduplicationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class DeduplicationServiceTest {

    private LeadRepository leadRepository;
    private DeduplicationService deduplicationService;

    @BeforeEach
    void setUp() {
        leadRepository = Mockito.mock(LeadRepository.class);
        deduplicationService = new DeduplicationService(leadRepository);
    }

    @Test
    void testCanonicalizeUrlStripsUtmAndTrailingSlashes() {
        String input = "https://EXAMPLE.com/jobs/dev/?utm_source=twitter&utm_medium=social#ref";
        String canonical = deduplicationService.canonicalizeUrl(input);
        assertEquals("https://example.com/jobs/dev", canonical);
    }

    @Test
    void testComputeFingerprintIsConsistentForVariations() {
        String fp1 = deduplicationService.computeFingerprint("Looking for React Dev!", "We need a frontend developer ASAP.", "https://site.com/1");
        String fp2 = deduplicationService.computeFingerprint("looking for react dev", "we need a frontend developer asap", "https://site.com/1");
        assertEquals(fp1, fp2);
        assertNotNull(fp1);
        assertEquals(64, fp1.length());
    }

    @Test
    void testIsDuplicateReturnsRepositoryValue() {
        when(leadRepository.existsByFingerprint("abc123hash")).thenReturn(true);
        assertTrue(deduplicationService.isDuplicate("abc123hash"));
        assertFalse(deduplicationService.isDuplicate("new_hash"));
    }
}
