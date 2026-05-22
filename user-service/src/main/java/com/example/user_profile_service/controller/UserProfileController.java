package com.example.user_profile_service.controller;

import com.example.user_profile_service.dto.UserProfileRequest;
import com.example.user_profile_service.dto.UserProfileResponse;
import com.example.user_profile_service.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService service;

    @GetMapping("/me")
    public Mono<UserProfileResponse> getProfile(JwtAuthenticationToken auth) {
        Jwt jwt = auth.getToken();
        return service.getOrCreate(
                jwt.getSubject(),
                jwt.getClaimAsString("email"),
                jwt.getClaimAsString("name"),
                jwt.getClaimAsString("phone_number")
        );
    }

    @PutMapping("/me")
    public Mono<UserProfileResponse> updateProfile(JwtAuthenticationToken auth,
                                                    @RequestBody UserProfileRequest request) {
        return service.update(auth.getToken().getSubject(), request);
    }
}
