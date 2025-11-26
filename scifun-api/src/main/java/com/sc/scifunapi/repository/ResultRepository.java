package com.sc.scifunapi.repository;

import com.sc.scifunapi.entity.Result;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ResultRepository extends MongoRepository<Result, String> {

    Result findByUserIdAndQuiz_Id(String userId, String quizId);

    List<Result> findByUserId(String userId);

    List<Result> findByQuiz_Id(String quizId);

    long countByQuiz_Id(String quizId);
}
