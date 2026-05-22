package com.example.user_profile_service.service;

import com.example.user_profile_service.dto.UserProfileRequest;
import com.example.user_profile_service.dto.UserProfileResponse;
import com.example.user_profile_service.model.UserProfile;
import com.example.user_profile_service.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserProfileRepository repository;

    public Mono<UserProfileResponse> getOrCreate(String userId, String email, String displayName, String phone) {
        return repository.findById(userId)
                .switchIfEmpty(repository.save(UserProfile.builder()
                        .id(userId)
                        .email(email)
                        .displayName(displayName != null ? displayName : "")
                        .phone(phone != null ? phone : "")
                        .onboarded(false)
                        .createdAt(Instant.now())
                        .updatedAt(Instant.now())
                        .build()))
                .map(this::toResponse);
    }

    public Mono<UserProfileResponse> update(String userId, UserProfileRequest request) {
        return repository.findById(userId)
                .flatMap(profile -> {
                    if (request.getDisplayName() != null) profile.setDisplayName(request.getDisplayName());
                    if (request.getPhone() != null) profile.setPhone(request.getPhone());
                    profile.setOnboarded(request.isOnboarded());
                    profile.setUpdatedAt(Instant.now());
                    return repository.save(profile);
                })
                .map(this::toResponse);
    }

    private UserProfileResponse toResponse(UserProfile p) {
        return UserProfileResponse.builder()
                .id(p.getId())
                .email(p.getEmail())
                .displayName(p.getDisplayName())
                .phone(p.getPhone())
                .onboarded(p.isOnboarded())
                .createdAt(p.getCreatedAt())
                .build();
    }
}
