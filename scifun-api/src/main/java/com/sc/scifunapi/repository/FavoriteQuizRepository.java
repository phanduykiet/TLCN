package com.sc.scifunapi.repository;

import com.sc.scifunapi.entity.FavoriteQuiz;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FavoriteQuizRepository extends MongoRepository<FavoriteQuiz, String> {

    Optional<FavoriteQuiz> findByUserAndQuiz(String user, String quiz);

    void deleteByUser(String userId);

    void deleteByUserAndQuiz(String user, String quiz);

    // Dùng cho list
    Page<FavoriteQuiz> findByUser(String user, Pageable pageable);

    Page<FavoriteQuiz> findByUserAndQuizIn(String user, List<String> quizIds, Pageable pageable);

    long countByUser(String user);

    long countByUserAndQuizIn(String user, List<String> quizIds);
}
