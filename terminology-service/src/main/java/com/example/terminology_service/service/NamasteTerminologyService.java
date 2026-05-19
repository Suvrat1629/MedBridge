package com.example.terminology_service.service;

import com.example.terminology_service.model.NamasteCode;
import com.example.terminology_service.repository.NamasteCodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NamasteTerminologyService {

    private final NamasteCodeRepository namasteCodeRepository;

    public Flux<NamasteCode> searchForAutoComplete(String searchTerm, int maxResults) {
        if (searchTerm == null || searchTerm.trim().length() < 2) {
            return Flux.empty();
        }
        return namasteCodeRepository
                .findByCodeTitleContainingIgnoreCase(searchTerm.trim())
                .take(maxResults);
    }

    @Cacheable(value = "namaste-code-lookup", key = "#namasteCode")
    public Mono<NamasteCode> getByNamasteCode(String namasteCode) {
        log.debug("Cache miss - getByNamasteCode: {}", namasteCode);
        return namasteCodeRepository.findByCode(namasteCode);
    }

    @Cacheable(value = "category-lookup", key = "#category")
    public Flux<NamasteCode> getByCategory(String category) {
        log.debug("Cache miss - getByCategory: {}", category);
        return namasteCodeRepository.findByType(category);
    }

    @Cacheable(value = "code-lookup", key = "#codeValue.trim().toLowerCase()")
    public Flux<NamasteCode> searchByCode(String codeValue) {
        if (codeValue == null || codeValue.trim().isEmpty()) {
            return Flux.empty();
        }
        String trimmed = codeValue.trim();
        log.debug("Cache miss - searchByCode: {}", trimmed);
        return namasteCodeRepository.findTopByCodeOrderByConfidenceScoreDesc(trimmed)
                .map(doc -> doc.getTm2Code() != null ? doc.getTm2Code().trim() : trimmed)
                .defaultIfEmpty(trimmed)
                .flatMapMany(namasteCodeRepository::findByAnyCode)
                .filter(code -> code.getConfidenceScore() != null && code.getConfidenceScore() > 0.6)
                .collectList()
                .flatMapMany(list -> {
                    Map<String, NamasteCode> bestPerType = list.stream()
                            .collect(Collectors.toMap(NamasteCode::getType,
                                    Function.identity(),
                                    (existing, incoming) -> incoming.getConfidenceScore() > existing.getConfidenceScore() ? incoming : existing));
                    return Flux.fromIterable(bestPerType.values());
                });
    }

    public Flux<NamasteCode> searchBySymptoms(String symptomQuery) {
        if (symptomQuery == null || symptomQuery.trim().length() < 2) {
            return Flux.empty();
        }
        return namasteCodeRepository.findBySymptoms(escapeRegexSpecialChars(symptomQuery.trim()));
    }

    private String escapeRegexSpecialChars(String input) {
        return input.replaceAll("([\\[\\]\\(\\)\\{\\}\\+\\*\\?\\^\\$\\|\\.])", "\\\\$1");
    }
}
