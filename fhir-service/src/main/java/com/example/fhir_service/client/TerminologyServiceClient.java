package com.example.fhir_service.client;

import com.example.fhir_service.dto.NamasteCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

@Service
@Slf4j
public class TerminologyServiceClient {

    private final WebClient webClient;
    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    public TerminologyServiceClient(WebClient.Builder webClientBuilder,
                                    @Value("${terminology-service.base-url}") String baseUrl) {
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
    }

    public Mono<List<NamasteCode>> searchByCode(String codeValue) {
        log.info("Calling terminology service - search by code: {}", codeValue);
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/internal/terminology/search/code/{codeValue}")
                        .build(codeValue))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<NamasteCode>>() {})
                .timeout(TIMEOUT)
                .defaultIfEmpty(List.of())
                .onErrorReturn(List.of());
    }

    public Mono<List<NamasteCode>> searchBySymptoms(List<String> symptoms) {
        String query = String.join(",", symptoms);
        log.info("Calling terminology service - search by symptoms: {}", query);
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/internal/terminology/search/symptoms")
                        .queryParam("query", query)
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<NamasteCode>>() {})
                .timeout(TIMEOUT)
                .defaultIfEmpty(List.of())
                .onErrorReturn(List.of());
    }
}
