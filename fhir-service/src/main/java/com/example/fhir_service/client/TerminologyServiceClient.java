package com.example.fhir_service.client;

import com.example.fhir_service.dto.NamasteCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreakerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.net.ConnectException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeoutException;

@Service
@Slf4j
public class TerminologyServiceClient {

    private final WebClient webClient;
    private final ReactiveCircuitBreaker circuitBreaker;

    private static final Duration TIMEOUT = Duration.ofSeconds(5);
    private static final Retry RETRY_SPEC = Retry
            .backoff(2, Duration.ofMillis(200))
            .jitter(0.5)
            .filter(TerminologyServiceClient::isTransientError)
            .onRetryExhaustedThrow((spec, signal) -> signal.failure());

    public TerminologyServiceClient(WebClient.Builder webClientBuilder,
                                    @Value("${terminology-service.base-url}") String baseUrl,
                                    ReactiveCircuitBreakerFactory<?, ?> circuitBreakerFactory) {
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
        this.circuitBreaker = circuitBreakerFactory.create("terminology-service");
    }

    public Mono<List<NamasteCode>> searchByCode(String codeValue) {
        log.info("Calling terminology service - search by code: {}", codeValue);
        return circuitBreaker.run(
                webClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/internal/terminology/search/code/{codeValue}")
                                .build(codeValue))
                        .retrieve()
                        .bodyToMono(new ParameterizedTypeReference<List<NamasteCode>>() {})
                        .timeout(TIMEOUT)
                        .retryWhen(RETRY_SPEC)
                        .defaultIfEmpty(List.of()),
                throwable -> {
                    log.warn("Circuit breaker fallback - searchByCode [{}]: {}", codeValue, throwable.getMessage());
                    return Mono.just(List.of());
                }
        );
    }

    public Mono<List<NamasteCode>> searchBySymptoms(List<String> symptoms) {
        String query = String.join(",", symptoms);
        log.info("Calling terminology service - search by symptoms: {}", query);
        return circuitBreaker.run(
                webClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/internal/terminology/search/symptoms")
                                .queryParam("query", query)
                                .build())
                        .retrieve()
                        .bodyToMono(new ParameterizedTypeReference<List<NamasteCode>>() {})
                        .timeout(TIMEOUT)
                        .retryWhen(RETRY_SPEC)
                        .defaultIfEmpty(List.of()),
                throwable -> {
                    log.warn("Circuit breaker fallback - searchBySymptoms [{}]: {}", query, throwable.getMessage());
                    return Mono.just(List.of());
                }
        );
    }

    private static boolean isTransientError(Throwable throwable) {
        if (throwable instanceof WebClientResponseException e) {
            return e.getStatusCode().is5xxServerError();
        }
        return throwable instanceof ConnectException
                || throwable instanceof TimeoutException;
    }
}
