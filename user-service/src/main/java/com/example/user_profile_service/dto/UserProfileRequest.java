package com.example.user_profile_service.dto;

import lombok.Data;

@Data
public class UserProfileRequest {
    private String displayName;
    private String phone;
    private boolean onboarded;
}
