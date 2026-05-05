package com.example.terminology_service.controller;

import com.example.terminology_service.model.NamasteCode;
import com.example.terminology_service.service.NamasteTerminologyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/internal/terminology")
@RequiredArgsConstructor
@Slf4j
public class InternalTerminologyController {

    private final NamasteTerminologyService terminologyService;

    @GetMapping("/search/code/{codeValue}")
    public Mono<ResponseEntity<List<NamasteCode>>> searchByCodeInternal(@PathVariable String codeValue) {
        log.info("Internal code search: {}", codeValue);
        return terminologyService.searchByCode(codeValue)
                .collectList()
                .map(ResponseEntity::ok)
                .onErrorReturn(ResponseEntity.ok(List.of()));
    }

    @GetMapping("/search/symptoms")
    public Mono<ResponseEntity<List<NamasteCode>>> searchBySymptomsInternal(@RequestParam String query) {
        log.info("Internal symptom search: {}", query);
        return terminologyService.searchBySymptoms(query)
                .collectList()
                .map(ResponseEntity::ok)
                .onErrorReturn(ResponseEntity.ok(List.of()));
    }

    @GetMapping("/code/{namasteCode}")
    public Mono<ResponseEntity<NamasteCode>> getByNamasteCodeInternal(@PathVariable String namasteCode) {
        log.info("Internal get code: {}", namasteCode);
        return terminologyService.getByNamasteCode(namasteCode)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build())
                .onErrorReturn(ResponseEntity.notFound().build());
    }

    @GetMapping("/category/{categoryType}")
    public Mono<ResponseEntity<List<NamasteCode>>> getByCategoryInternal(@PathVariable String categoryType) {
        log.info("Internal category search: {}", categoryType);
        return terminologyService.getByCategory(categoryType)
                .collectList()
                .map(ResponseEntity::ok)
                .onErrorReturn(ResponseEntity.ok(List.of()));
    }

    @GetMapping("/autocomplete")
    public Mono<ResponseEntity<List<NamasteCode>>> autoCompleteInternal(
            @RequestParam String query,
            @RequestParam(defaultValue = "10") int limit) {
        log.info("Internal autocomplete: {} limit={}", query, limit);
        return terminologyService.searchForAutoComplete(query, limit)
                .collectList()
                .map(ResponseEntity::ok)
                .onErrorReturn(ResponseEntity.ok(List.of()));
    }

    @GetMapping("/health")
    public Mono<ResponseEntity<String>> healthInternal() {
        return Mono.just(ResponseEntity.ok("TERMINOLOGY_SERVICE_UP"));
    }
}
