package com.example.user_profile_service.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class UserProfileResponse {
    private String id;
    private String email;
    private String displayName;
    private String phone;
    private boolean onboarded;
    private Instant createdAt;
}
