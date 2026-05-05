package com.example.fhir_service.service;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import com.example.fhir_service.client.TerminologyServiceClient;
import com.example.fhir_service.dto.NamasteCode;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.IntegerType;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TerminologyFhirServiceTest {

    private TerminologyFhirService svc;
    private TerminologyServiceClient mockClient;
    private IParser jsonParser;

    @BeforeEach
    public void setup() {
        FhirContext fhirContext = FhirContext.forR4();
        jsonParser = fhirContext.newJsonParser();
        mockClient = mock(TerminologyServiceClient.class);
        svc = new TerminologyFhirService(mockClient, fhirContext, jsonParser);
    }

    @Test
    public void testToJson_serializesResource() {
        Patient patient = new Patient();
        patient.setId("p1");
        String json = svc.toJson(patient);
        assertNotNull(json);
        assertTrue(json.contains("\"resourceType\":\"Patient\""));
    }

    @Test
    public void testCreateSearchByCodeResult_noResults() {
        when(mockClient.searchByCode(anyString())).thenReturn(Mono.just(List.of()));

        Parameters p = svc.createSearchByCodeResult("NONEXISTENT").block();
        assertNotNull(p);

        var resultParam = p.getParameter().stream()
                .filter(pp -> "result".equals(pp.getName()))
                .findFirst()
                .orElse(null);
        assertNotNull(resultParam);
        assertFalse(((BooleanType) resultParam.getValue()).booleanValue());
    }

    @Test
    public void testCreateSearchBySymptomsResult_groupingAndLimits() {
        NamasteCode a = new NamasteCode();
        a.setTm2Code("TM2.G1");
        a.setTm2Title("Disease G1");
        a.setTm2Definition("Def G1");
        a.setConfidenceScore(0.9);
        a.setCode("A1");
        a.setCodeTitle("Map A1");

        NamasteCode b = new NamasteCode();
        b.setTm2Code("TM2.G1");
        b.setTm2Title("Disease G1");
        b.setTm2Definition("Def G1");
        b.setConfidenceScore(0.8);
        b.setCode("B1");
        b.setCodeTitle("Map B1");

        when(mockClient.searchBySymptoms(any())).thenReturn(Mono.just(List.of(a, b)));

        Parameters p = svc.createSearchBySymptomsResult(List.of("fever", "headache")).block();
        assertNotNull(p);

        var resultParam = p.getParameter().stream()
                .filter(pp -> "result".equals(pp.getName()))
                .findFirst().orElse(null);
        assertNotNull(resultParam);
        assertTrue(((BooleanType) resultParam.getValue()).booleanValue());

        var totalGroupsParam = p.getParameter().stream()
                .filter(pp -> "totalDiseaseGroups".equals(pp.getName()))
                .findFirst().orElse(null);
        assertNotNull(totalGroupsParam);
        assertEquals(1, ((IntegerType) totalGroupsParam.getValue()).getValue().intValue());

        assertTrue(p.getParameter().stream().anyMatch(pp -> "diseaseGroup".equals(pp.getName())));
    }

    @Test
    public void testCreateSearchBySymptomsResult_empty_returnsNotFound() {
        Parameters p = svc.createSearchBySymptomsResult(List.of()).block();
        assertNotNull(p);
        var resultParam = p.getParameter().stream()
                .filter(pp -> "result".equals(pp.getName()))
                .findFirst().orElse(null);
        assertNotNull(resultParam);
        assertFalse(((BooleanType) resultParam.getValue()).booleanValue());
    }
}
