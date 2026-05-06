package com.example.api_gateway.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/gateway")
@Slf4j
public class GatewayInfoController {

    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "api-gateway");
        health.put("timestamp", System.currentTimeMillis());
        return health;
    }

    @GetMapping("/info")
    public Map<String, Object> info() {
        Map<String, Object> info = new HashMap<>();
        info.put("name", "MedBridge API Gateway");
        info.put("version", "2.0.0");
        info.put("description", "WebFlux Gateway — JWT validation via Keycloak JWKS");

        Map<String, String> routes = new HashMap<>();
        routes.put("FHIR (Protected)", "/api/fhir/** → lb://fhir-service");
        routes.put("Terminology (Public)", "/api/terminology/** → lb://terminology-service");

        info.put("routes", routes);
        return info;
    }
}
