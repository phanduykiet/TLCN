package com.sc.scifunapi.controller;

import com.sc.scifunapi.dto.quiz.QuizDetailDTO;
import com.sc.scifunapi.entity.Quiz;
import com.sc.scifunapi.service.QuizService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/quiz")
@RequiredArgsConstructor
public class QuizController {

    private final QuizService quizService;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/sync-all-quizzes-es")
    public ResponseEntity<Map<String, Object>> syncAllQuizzesToES() {
        try {
            quizService.reindexAllQuizzes();
            return ResponseEntity.ok(Map.of(
                    "status", 200,
                    "message", "Đồng bộ lại toàn bộ Quiz lên Elasticsearch thành công"
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.status(500).body(Map.of(
                    "status", 500,
                    "message", "Lỗi khi đồng bộ Elasticsearch: " + e.getMessage()
            ));
        }
    }


    // Thêm Quiz
    @PostMapping("/create-quiz")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> createQuiz(
            @RequestBody Map<String, Object> body
    ) {
        try {
            Map<String, Object> quiz = quizService.createQuizSv(body);
            return ResponseEntity.ok(Map.of(
                    "status", 200,
                    "message", "Thêm thành công",
                    "data", quiz
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", 400,
                    "message", e.getMessage()
            ));
        }
    }

    // Lấy danh sách Quiz (phân trang, lọc theo topic, search theo title)
    @GetMapping("/get-quizzes")
    public ResponseEntity<Map<String, Object>> getQuizzes(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String topicId,
            @RequestParam(required = false) String search
    ) {
        try {
            Map<String, Object> data = quizService.getQuizzes(page, limit, topicId, search);
            return ResponseEntity.ok(
                    Map.of(
                            "status", 200,
                            "message", "Lấy danh sách thành công",
                            "data", data
                    )
            );
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    Map.of(
                            "status", 400,
                            "message", e.getMessage()
                    )
            );
        }
    }

    // QuizController.java
    @GetMapping("/get-trend-quizzes")
    public ResponseEntity<Map<String, Object>> getTrendingQuizzes(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) Double timeWeight,
            @RequestParam(required = false) Double popularityWeight
    ) {
        try {
            double tw = timeWeight != null ? timeWeight : 0.6;
            double pw = popularityWeight != null ? popularityWeight : 0.4;

            Map<String, Object> data = quizService.getTrendingQuizzes(page, limit, tw, pw);

            return ResponseEntity.ok(Map.of(
                    "status", 200,
                    "message", "Lấy danh sách thành công",
                    "data", data
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", 400,
                    "message", e.getMessage()
            ));
        }
    }

    // Lấy chi tiết Quiz
    @GetMapping("/get-quizById/{id}")
    public ResponseEntity<Map<String, Object>> getQuizById(@PathVariable("id") String id) {
        try {
            QuizDetailDTO quizDto = quizService.getQuizById(id);

            return ResponseEntity.ok(Map.of(
                    "status", 200,
                    "message", "Lấy chi tiết thành công",
                    "data", quizDto
            ));
        } catch (Exception e) {
            return ResponseEntity.status(400).body(Map.of(
                    "status", 400,
                    "message", e.getMessage()
            ));
        }
    }

    //Cập nhật quiz
    @PutMapping("/update-quiz/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> updateQuiz(
            @PathVariable String id,
            @RequestBody Map<String, Object> body
    ) {
        try {
            Map<String, Object> quiz = quizService.updateQuizSv(id, body);

            return ResponseEntity.ok(Map.of(
                    "status", 200,
                    "message", "Cập nhật thành công",
                    "data", quiz
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", 400,
                    "message", e.getMessage()
            ));
        }
    }

    //xóa quiz
    // Xóa Quiz
    @DeleteMapping("/delete-quiz/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> deleteQuiz(@PathVariable String id) {
        try {
            Map<String, Object> result = quizService.deleteQuizSv(id);

            return ResponseEntity.ok(Map.of(
                    "status", 200,
                    "message", "Xóa thành công",
                    "data", result
            ));

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", 400,
                    "message", e.getMessage()
            ));
        }
    }


}
