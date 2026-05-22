package com.example.user_profile_service.repository;

import com.example.user_profile_service.model.UserProfile;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserProfileRepository extends ReactiveMongoRepository<UserProfile, String> {
}
