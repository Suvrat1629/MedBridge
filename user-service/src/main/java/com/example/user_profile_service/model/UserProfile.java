package com.example.user_profile_service.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "user_profiles")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfile {

    @Id
    private String id; // Keycloak sub

    private String email;
    private String displayName;
    private String phone;
    private boolean onboarded;
    private Instant createdAt;
    private Instant updatedAt;
}
