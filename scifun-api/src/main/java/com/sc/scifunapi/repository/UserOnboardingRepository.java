package com.sc.scifunapi.repository;

import com.sc.scifunapi.entity.UserOnboarding;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserOnboardingRepository extends MongoRepository<UserOnboarding, String> {

    Optional<UserOnboarding> findByUserId(String userId);

    boolean existsByUserId(String userId);

    void deleteByUserId(String userId);
}