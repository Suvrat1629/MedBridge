package com.example.fhir_service.service;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import com.example.fhir_service.client.TerminologyServiceClient;
import com.example.fhir_service.dto.DiseaseMapping;
import com.example.fhir_service.dto.NamasteCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TerminologyFhirService {

    private final TerminologyServiceClient terminologyServiceClient;
    private final FhirContext fhirContext;
    private final IParser jsonParser;

    // Virtual-thread scheduler for blocking HAPI FHIR operations
    private static final Scheduler FHIR_SCHEDULER =
            Schedulers.fromExecutor(Executors.newVirtualThreadPerTaskExecutor());

    public String toJson(Resource resource) {
        return jsonParser.encodeResourceToString(resource);
    }

    public Mono<Parameters> createSearchByCodeResult(String codeValue) {
        log.info("Creating FHIR Parameters for code search: {}", codeValue);
        return terminologyServiceClient.searchByCode(codeValue)
                .flatMap(result -> Mono.fromCallable(() -> buildSearchByCodeParams(codeValue, result))
                        .subscribeOn(FHIR_SCHEDULER));
    }

    public Mono<Parameters> createSearchBySymptomsResult(List<String> symptoms) {
        if (symptoms == null || symptoms.isEmpty()) {
            return Mono.fromCallable(() -> {
                Parameters parameters = new Parameters();
                parameters.setId("search-by-symptoms-result-" + System.currentTimeMillis());
                parameters.addParameter("result", new BooleanType(false));
                parameters.addParameter("message", new StringType("No symptoms provided"));
                return parameters;
            }).subscribeOn(FHIR_SCHEDULER);
        }
        log.info("Creating FHIR Parameters for symptoms search: {}", symptoms);
        return terminologyServiceClient.searchBySymptoms(symptoms)
                .flatMap(allResults -> Mono.fromCallable(() -> buildSearchBySymptomsParams(symptoms, allResults))
                        .subscribeOn(FHIR_SCHEDULER));
    }

    private Parameters buildSearchByCodeParams(String codeValue, List<NamasteCode> result) {
        Parameters parameters = new Parameters();
        parameters.setId("search-by-code-result-" + codeValue);

        if (result.isEmpty()) {
            parameters.addParameter("result", new BooleanType(false));
            parameters.addParameter("message", new StringType("No code found matching: " + codeValue));
            return parameters;
        }

        parameters.addParameter("result", new BooleanType(true));
        parameters.addParameter("totalMatches", new IntegerType(result.size()));

        for (NamasteCode namasteCode : result) {
            Parameters.ParametersParameterComponent matchGroup = new Parameters.ParametersParameterComponent();
            matchGroup.setName("match");

            Parameters.ParametersParameterComponent codeParam = new Parameters.ParametersParameterComponent();
            codeParam.setName("code");
            codeParam.addPart().setName("system").setValue(new UriType("http://terminology.hl7.org.in/CodeSystem/namaste"));
            codeParam.addPart().setName("code").setValue(new CodeType(namasteCode.getNamasteCode()));
            codeParam.addPart().setName("display").setValue(new StringType(namasteCode.getNamasteName()));
            matchGroup.addPart(codeParam);

            matchGroup.addPart().setName("type").setValue(new StringType(namasteCode.getNamasteCategory()));

            if (namasteCode.getIcd11Tm2Code() != null) {
                Parameters.ParametersParameterComponent tm2Param = new Parameters.ParametersParameterComponent();
                tm2Param.setName("tm2Mapping");
                tm2Param.addPart().setName("system").setValue(new UriType("http://id.who.int/icd/release/11/tm2"));
                tm2Param.addPart().setName("code").setValue(new CodeType(namasteCode.getIcd11Tm2Code()));
                tm2Param.addPart().setName("display").setValue(new StringType(namasteCode.getIcd11Tm2Name()));
                tm2Param.addPart().setName("definition").setValue(new StringType(namasteCode.getIcd11Tm2Description()));
                tm2Param.addPart().setName("link").setValue(new UriType(namasteCode.getIcd11Tm2Uri()));
                matchGroup.addPart(tm2Param);
            }

            if (namasteCode.getNamasteDescription() != null) {
                matchGroup.addPart().setName("description").setValue(new StringType(namasteCode.getNamasteDescription()));
            }
            if (namasteCode.getConfidenceScore() != null) {
                matchGroup.addPart().setName("confidenceScore").setValue(new DecimalType(namasteCode.getConfidenceScore()));
            }

            parameters.addParameter(matchGroup);
        }

        return parameters;
    }

    private Parameters buildSearchBySymptomsParams(List<String> symptoms, List<NamasteCode> allResults) {
        if (allResults.isEmpty()) {
            Parameters parameters = new Parameters();
            parameters.setId("search-by-symptoms-result-" + System.currentTimeMillis());
            parameters.addParameter("result", new BooleanType(false));
            parameters.addParameter("message", new StringType("No symptoms found matching: " + String.join(", ", symptoms)));
            return parameters;
        }

        Map<String, List<NamasteCode>> groupedByTm2 = allResults.stream()
                .filter(code -> code.getTm2Code() != null)
                .collect(Collectors.groupingBy(NamasteCode::getTm2Code));

        List<DiseaseMapping> groupedResults = groupedByTm2.entrySet().stream()
                .map(entry -> {
                    DiseaseMapping diseaseMapping = new DiseaseMapping();
                    NamasteCode firstCode = entry.getValue().get(0);
                    diseaseMapping.setTm2Code(entry.getKey());
                    diseaseMapping.setTm2Title(firstCode.getTm2Title());
                    diseaseMapping.setTm2Definition(firstCode.getTm2Definition());
                    diseaseMapping.setSimilarityScore(firstCode.getConfidenceScore());
                    diseaseMapping.setMappings(entry.getValue());
                    return diseaseMapping;
                })
                .sorted((a, b) -> Double.compare(
                        b.getSimilarityScore() != null ? b.getSimilarityScore() : 0.0,
                        a.getSimilarityScore() != null ? a.getSimilarityScore() : 0.0))
                .collect(Collectors.toList());

        if (groupedResults.size() > 20) {
            Parameters parameters = new Parameters();
            parameters.setId("search-by-symptoms-error-" + System.currentTimeMillis());
            parameters.addParameter("result", new BooleanType(false));
            parameters.addParameter("error", new StringType("Too many results"));
            parameters.addParameter("message", new StringType("Found " + groupedResults.size() + " disease groups. Please refine your symptoms to get 20 or fewer results."));
            parameters.addParameter("resultCount", new IntegerType(groupedResults.size()));
            parameters.addParameter("maxAllowed", new IntegerType(20));
            return parameters;
        }

        Parameters parameters = new Parameters();
        parameters.setId("search-by-symptoms-grouped-results-" + System.currentTimeMillis());
        parameters.addParameter("result", new BooleanType(true));
        parameters.addParameter("totalDiseaseGroups", new IntegerType(groupedResults.size()));
        parameters.addParameter("matchedSymptoms", new StringType(String.join(", ", symptoms)));

        for (DiseaseMapping diseaseMapping : groupedResults) {
            Parameters.ParametersParameterComponent diseaseGroup = new Parameters.ParametersParameterComponent();
            diseaseGroup.setName("diseaseGroup");

            Parameters.ParametersParameterComponent tm2Info = new Parameters.ParametersParameterComponent();
            tm2Info.setName("tm2Disease");
            tm2Info.addPart().setName("system").setValue(new UriType("http://id.who.int/icd/release/11/tm2"));
            tm2Info.addPart().setName("code").setValue(new CodeType(diseaseMapping.getTm2Code()));
            tm2Info.addPart().setName("display").setValue(new StringType(diseaseMapping.getTm2Title()));
            if (diseaseMapping.getTm2Definition() != null) {
                tm2Info.addPart().setName("definition").setValue(new StringType(diseaseMapping.getTm2Definition()));
            }
            diseaseGroup.addPart(tm2Info);

            if (diseaseMapping.getSimilarityScore() != null) {
                diseaseGroup.addPart().setName("symptomSimilarityScore").setValue(new DecimalType(diseaseMapping.getSimilarityScore()));
            }
            diseaseGroup.addPart().setName("traditionalMedicineMappingCount").setValue(new IntegerType(diseaseMapping.getMappingCount()));

            for (NamasteCode mapping : diseaseMapping.getMappings()) {
                Parameters.ParametersParameterComponent mappingParam = new Parameters.ParametersParameterComponent();
                mappingParam.setName("traditionalMedicineMapping");

                Parameters.ParametersParameterComponent codeParam = new Parameters.ParametersParameterComponent();
                codeParam.setName("code");
                codeParam.addPart().setName("system").setValue(new UriType("http://terminology.hl7.org.in/CodeSystem/namaste"));
                codeParam.addPart().setName("code").setValue(new CodeType(mapping.getNamasteCode()));
                codeParam.addPart().setName("display").setValue(new StringType(mapping.getNamasteName()));
                mappingParam.addPart(codeParam);

                mappingParam.addPart().setName("type").setValue(new StringType(mapping.getNamasteCategory()));
                if (mapping.getNamasteDescription() != null) {
                    mappingParam.addPart().setName("description").setValue(new StringType(mapping.getNamasteDescription()));
                }
                if (mapping.getConfidenceScore() != null) {
                    mappingParam.addPart().setName("mappingConfidenceScore").setValue(new DecimalType(mapping.getConfidenceScore()));
                }

                diseaseGroup.addPart(mappingParam);
            }

            parameters.addParameter(diseaseGroup);
        }

        return parameters;
    }
}
