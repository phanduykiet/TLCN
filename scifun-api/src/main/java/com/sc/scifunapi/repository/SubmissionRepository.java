package com.sc.scifunapi.repository;

import com.sc.scifunapi.entity.Submission;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubmissionRepository extends MongoRepository<Submission, String> {

    List<Submission> findByUserId(String userId);

    List<Submission> findByQuiz_Id(String quizId);

    List<Submission> findByUserIdAndQuiz_Id(String userId, String quizId);

    void deleteByUserId(String userId);
}
