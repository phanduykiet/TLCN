package com.sc.scifunapi.repository;

import com.sc.scifunapi.entity.Question;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

public interface QuestionRepository extends MongoRepository<Question, String> {
    // Lọc theo quizId + phân trang
    Page<Question> findByQuiz_Id(String quizId, Pageable pageable);

    // Lấy tất cả + phân trang (khi không truyền quizId)
    Page<Question> findAll(Pageable pageable);

    @Query("{ $or: [ { text: { $regex: ?0, $options: 'i' } }, { explanation: { $regex: ?0, $options: 'i' } } ] }")
    List<Question> searchByKeyword(String keyword, Pageable pageable);
}
