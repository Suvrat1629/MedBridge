package com.example.fhir_service.controller;

import com.example.fhir_service.service.TerminologyFhirService;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/fhir")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "FHIR Terminology", description = "FHIR R4 compliant terminology operations for traditional medicine codes and ICD-11 TM2 mappings")
public class FhirTerminologyController {

    private final TerminologyFhirService terminologyFhirService;

    private static final String FHIR_JSON_CONTENT_TYPE = "application/fhir+json;fhirVersion=4.0";
    private static final Scheduler FHIR_SCHEDULER =
        Schedulers.fromExecutor(Executors.newVirtualThreadPerTaskExecutor());

    @Operation(summary = "Search by Medical Code", description = "Searches for terminology entries using medical codes. Returns FHIR Parameters resource with matching results.")
    @GetMapping(value = "/search/code/{codeValue}", produces = FHIR_JSON_CONTENT_TYPE)
    public Mono<ResponseEntity<String>> searchByCode(
            @Parameter(description = "Medical code to search for (e.g., A01.1, TM2.001.001)", example = "A01.1", required = true)
            @PathVariable String codeValue) {

        log.info("FHIR search by code: {}", codeValue);
        return terminologyFhirService.createSearchByCodeResult(codeValue)
                .map(parameters -> {
                    addFhirMetadata(parameters);
                    return createFhirResponse(terminologyFhirService.toJson(parameters));
                })
                .onErrorResume(e -> {
                    log.error("Error in FHIR code search", e);
                    return Mono.fromCallable(() -> createFhirErrorResponse("Code search failed", e.getMessage()))
                            .subscribeOn(FHIR_SCHEDULER);
                });
    }

    @Operation(summary = "Search by TM2 Code", description = "Searches for terminology entries using TM2 codes specifically (ICD-11 Traditional Medicine codes).")
    @GetMapping(value = "/search/tm2code/{codeValue}", produces = FHIR_JSON_CONTENT_TYPE)
    public Mono<ResponseEntity<String>> searchByTm2Code(
            @Parameter(description = "TM2 code to search for (ICD-11 Traditional Medicine code)", example = "TM2.001.001", required = true)
            @PathVariable String codeValue) {

        log.info("FHIR search by TM2 code: {}", codeValue);
        return terminologyFhirService.createSearchByCodeResult(codeValue)
                .map(parameters -> {
                    addFhirMetadata(parameters);
                    return createFhirResponse(terminologyFhirService.toJson(parameters));
                })
                .onErrorResume(e -> {
                    log.error("Error in FHIR TM2 code search", e);
                    return Mono.fromCallable(() -> createFhirErrorResponse("TM2 code search failed", e.getMessage()))
                            .subscribeOn(FHIR_SCHEDULER);
                });
    }

    @Operation(summary = "Search by Standard Medical Code", description = "Searches for terminology entries using standard medical codes (non-TM2 codes).")
    @GetMapping(value = "/search/codeonly/{codeValue}", produces = FHIR_JSON_CONTENT_TYPE)
    public Mono<ResponseEntity<String>> searchByCodeOnly(
            @Parameter(description = "Standard medical code to search for", example = "A01.1", required = true)
            @PathVariable String codeValue) {

        log.info("FHIR search by code only: {}", codeValue);
        return terminologyFhirService.createSearchByCodeResult(codeValue)
                .map(parameters -> {
                    addFhirMetadata(parameters);
                    return createFhirResponse(terminologyFhirService.toJson(parameters));
                })
                .onErrorResume(e -> {
                    log.error("Error in FHIR code only search", e);
                    return Mono.fromCallable(() -> createFhirErrorResponse("Code only search failed", e.getMessage()))
                            .subscribeOn(FHIR_SCHEDULER);
                });
    }

    @Operation(summary = "Search by Symptoms (GET)", description = "Searches for terminology entries based on symptoms. Accepts comma-separated or space-separated symptoms.")
    @GetMapping(value = "/search/symptoms", produces = FHIR_JSON_CONTENT_TYPE)
    public Mono<ResponseEntity<String>> searchBySymptoms(
            @Parameter(description = "Symptoms to search for, separated by commas or spaces", example = "fever,headache,nausea", required = true)
            @RequestParam String query) {

        log.info("FHIR search by symptoms: {}", query);
        List<String> symptoms = parseSymptoms(query);
        return terminologyFhirService.createSearchBySymptomsResult(symptoms)
                .map(parameters -> {
                    addFhirMetadata(parameters);
                    return createFhirResponse(terminologyFhirService.toJson(parameters));
                })
                .onErrorResume(e -> {
                    log.error("Error in FHIR symptom search", e);
                    return Mono.fromCallable(() -> createFhirErrorResponse("Symptom search failed", e.getMessage()))
                            .subscribeOn(FHIR_SCHEDULER);
                });
    }

    @Operation(summary = "Search by Symptoms (POST)", description = "Searches for terminology entries based on symptoms provided as JSON array.")
    @PostMapping(value = "/search/symptoms", produces = FHIR_JSON_CONTENT_TYPE, consumes = "application/json")
    public Mono<ResponseEntity<String>> searchBySymptomsPost(
            @RequestBody Map<String, List<String>> requestBody) {

        log.info("FHIR POST search by symptoms: {}", requestBody);
        List<String> symptoms = requestBody.get("symptoms");
        if (symptoms == null || symptoms.isEmpty()) {
            return Mono.fromCallable(() -> createFhirErrorResponse("Invalid request", "symptoms array is required"))
                    .subscribeOn(FHIR_SCHEDULER);
        }
        return terminologyFhirService.createSearchBySymptomsResult(symptoms)
                .map(parameters -> {
                    addFhirMetadata(parameters);
                    return createFhirResponse(terminologyFhirService.toJson(parameters));
                })
                .onErrorResume(e -> {
                    log.error("Error in FHIR symptom POST search", e);
                    return Mono.fromCallable(() -> createFhirErrorResponse("Symptom search failed", e.getMessage()))
                            .subscribeOn(FHIR_SCHEDULER);
                });
    }

    @Hidden
    @GetMapping(value = "/metadata", produces = FHIR_JSON_CONTENT_TYPE)
    public Mono<ResponseEntity<String>> getCapabilityStatement() {
        log.info("FHIR capability statement requested");
        return Mono.fromCallable(() -> {
            CapabilityStatement capabilityStatement = createCapabilityStatement();
            addFhirMetadata(capabilityStatement);
            return createFhirResponse(terminologyFhirService.toJson(capabilityStatement));
        })
        .subscribeOn(FHIR_SCHEDULER)
        .onErrorResume(e -> {
            log.error("Error creating capability statement", e);
            return Mono.fromCallable(() -> createFhirErrorResponse("Capability statement failed", e.getMessage()))
                    .subscribeOn(FHIR_SCHEDULER);
        });
    }

    @Hidden
    @GetMapping(value = "/health", produces = "application/json")
    public Mono<ResponseEntity<Map<String, Object>>> health() {
        return Mono.just(ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "fhir-service",
                "timestamp", new Date()
        )));
    }

    private List<String> parseSymptoms(String query) {
        if (query == null || query.trim().isEmpty()) {
            return List.of();
        }
        if (query.contains(",")) {
            return Arrays.stream(query.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
        }
        return List.of(query.trim());
    }

    private ResponseEntity<String> createFhirResponse(String fhirJson) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.valueOf(FHIR_JSON_CONTENT_TYPE));
        headers.add("X-FHIR-Version", "4.0.1");
        return ResponseEntity.ok().headers(headers).body(fhirJson);
    }

    private ResponseEntity<String> createFhirErrorResponse(String message, String details) {
        try {
            OperationOutcome errorOutcome = new OperationOutcome();
            errorOutcome.setId("error-" + System.currentTimeMillis());

            OperationOutcome.OperationOutcomeIssueComponent errorIssue =
                    new OperationOutcome.OperationOutcomeIssueComponent();
            errorIssue.setSeverity(OperationOutcome.IssueSeverity.ERROR);
            errorIssue.setCode(OperationOutcome.IssueType.PROCESSING);
            errorIssue.setDiagnostics("Error: " + message + ". Details: " + details);
            errorOutcome.addIssue(errorIssue);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.valueOf(FHIR_JSON_CONTENT_TYPE));
            headers.add("X-FHIR-Version", "4.0.1");
            return ResponseEntity.badRequest().headers(headers)
                    .body(terminologyFhirService.toJson(errorOutcome));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .contentType(MediaType.valueOf(FHIR_JSON_CONTENT_TYPE))
                    .body("{\"resourceType\":\"OperationOutcome\",\"issue\":[{\"severity\":\"error\",\"code\":\"processing\",\"diagnostics\":\"" + message + "\"}]}");
        }
    }

    private void addFhirMetadata(DomainResource resource) {
        Meta meta = new Meta();
        meta.setVersionId("1");
        meta.setLastUpdated(new Date());
        meta.addProfile("http://hl7.org.in/fhir/StructureDefinition/AyushTerminology");
        meta.addSecurity()
                .setSystem("http://terminology.hl7.org/CodeSystem/v3-Confidentiality")
                .setCode("N")
                .setDisplay("Normal");
        meta.addTag()
                .setSystem("http://terminology.hl7.org.in/CodeSystem/terminology-tags")
                .setCode("terminology-service")
                .setDisplay("Terminology Service");
        resource.setMeta(meta);
    }

    private void addFhirMetadata(Parameters parameters) {
        Meta meta = new Meta();
        meta.setVersionId("1");
        meta.setLastUpdated(new Date());
        meta.addProfile("http://hl7.org.in/fhir/StructureDefinition/AyushParameters");
        meta.addSecurity()
                .setSystem("http://terminology.hl7.org/CodeSystem/v3-Confidentiality")
                .setCode("N")
                .setDisplay("Normal");
        meta.addTag()
                .setSystem("http://terminology.hl7.org.in/CodeSystem/terminology-tags")
                .setCode("terminology-operation")
                .setDisplay("Terminology Operation");
        parameters.setMeta(meta);
    }

    private CapabilityStatement createCapabilityStatement() {
        CapabilityStatement cs = new CapabilityStatement();
        cs.setId("namaste-fhir-terminology-server");
        cs.setUrl("http://terminology.hl7.org.in/fhir/CapabilityStatement/namaste-fhir-terminology-server");
        cs.setVersion("1.0.0");
        cs.setName("NamasteFhirTerminologyServer");
        cs.setTitle("Namaste FHIR Terminology Server");
        cs.setStatus(Enumerations.PublicationStatus.ACTIVE);
        cs.setDate(new Date());
        cs.setPublisher("Namaste Health Solutions");
        cs.setDescription("FHIR R4 compliant terminology server for traditional medicine codes and ICD-11 TM2 mappings");
        cs.setKind(CapabilityStatement.CapabilityStatementKind.INSTANCE);
        cs.setSoftware(new CapabilityStatement.CapabilityStatementSoftwareComponent()
                .setName("Namaste FHIR Service").setVersion("1.0.0"));
        cs.setImplementation(new CapabilityStatement.CapabilityStatementImplementationComponent()
                .setDescription("Namaste FHIR Terminology Service Implementation")
                .setUrl("http://localhost:8083/api/fhir"));
        cs.setFhirVersion(Enumerations.FHIRVersion._4_0_1);
        cs.addFormat("application/fhir+json");
        cs.addFormat("json");

        CapabilityStatement.CapabilityStatementRestComponent rest = new CapabilityStatement.CapabilityStatementRestComponent();
        rest.setMode(CapabilityStatement.RestfulCapabilityMode.SERVER);
        rest.setDocumentation("Traditional Medicine Terminology Operations");
        rest.addOperation().setName("search-by-code")
                .setDefinition("http://terminology.hl7.org.in/fhir/OperationDefinition/search-by-code");
        rest.addOperation().setName("search-by-symptoms")
                .setDefinition("http://terminology.hl7.org.in/fhir/OperationDefinition/search-by-symptoms");
        cs.addRest(rest);

        return cs;
    }
}
