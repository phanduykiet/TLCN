package com.sc.scifunapi.repository;

import com.sc.scifunapi.entity.UserProgress;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserProgressRepository extends MongoRepository<UserProgress, String> {

    Optional<UserProgress> findByUserIdAndSubjectId(String userId, String subjectId);

    List<UserProgress> findByUserId(String userId);

    List<UserProgress> findBySubjectId(String subjectId);
}
