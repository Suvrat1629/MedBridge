package com.example.terminology_service.repository;

import com.example.terminology_service.model.NamasteCode;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface NamasteCodeRepository extends ReactiveMongoRepository<NamasteCode, String> {

    @Query(value = "{'$or': [{'tm2_code': ?0}, {'code': ?0}]}", sort = "{'confidence_score': -1}")
    Flux<NamasteCode> findByAnyCode(@Param("codeValue") String codeValue);

    @Query("{'$or': [" +
            "{'code_description': {$regex: ?0, $options: 'i'}}, " +
            "{'tm2_definition': {$regex: ?0, $options: 'i'}}, " +
            "{'tm2_title': {$regex: ?0, $options: 'i'}}, " +
            "{'code_title': {$regex: ?0, $options: 'i'}}" +
            "]}")
    Flux<NamasteCode> findBySymptoms(@Param("symptomQuery") String symptomQuery);

    Mono<NamasteCode> findByCode(String code);

    @Query("{'code_title': {$regex: ?0, $options: 'i'}}")
    Flux<NamasteCode> findByCodeTitleContainingIgnoreCase(@Param("query") String query);

    Flux<NamasteCode> findByType(String type);

    Mono<NamasteCode> findTopByCodeOrderByConfidenceScoreDesc(String code);
}
