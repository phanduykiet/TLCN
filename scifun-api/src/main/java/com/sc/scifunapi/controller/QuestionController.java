package com.sc.scifunapi.controller;

import com.sc.scifunapi.service.QuestionService;
import com.sc.scifunapi.service.QuizService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/question")
@RequiredArgsConstructor
public class QuestionController {
    private final QuestionService questionService;

    // tạo câu hỏi
    @PostMapping("/create-question")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createQuestion(@RequestBody Map<String, Object> body) {
        try {
            Map<String, Object> question = questionService.createQuestionSv(body);

            return ResponseEntity.ok(Map.of(
                    "status", 200,
                    "message", "Thêm thành công",
                    "data", question
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", 400,
                    "message", e.getMessage()
            ));
        }
    }

    // Lấy danh sách câu hỏi
    @GetMapping("/get-questions")
    public ResponseEntity<Map<String, Object>> getQuestions(
            @RequestParam(required = true) Integer page,
            @RequestParam(required = true) Integer limit,
            @RequestParam(required = false) String quizId,
            HttpServletRequest request
    ) {
        try {
            // optionalAuth → userId có thể null
            String userId = (String) request.getAttribute("optionalUserId");

            Map<String, Object> result =
                    questionService.getQuestionsSv(page, limit, quizId, userId);

            return ResponseEntity.ok(
                    Map.of(
                            "status", 200,
                            "message", "Lấy danh sách thành công",
                            "data", result
                    )
            );

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    Map.of("status", 400, "message", e.getMessage())
            );
        }
    }

    // lấy chi tiết câu hỏi
    @GetMapping("/get-questionById/{_id}")
    public ResponseEntity<Map<String, Object>> getQuestionById(
            @PathVariable("_id") String id
    ) {
        try {
            Map<String, Object> question = questionService.getQuestionByIdSv(id);

            return ResponseEntity.ok(
                    Map.of(
                            "status", 200,
                            "message", "Lấy chi tiết thành công",
                            "data", question
                    )
            );
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(
                    Map.of(
                            "status", 400,
                            "message", e.getMessage()
                    )
            );
        }
    }

    // Cập nhật câu hỏi
    @PutMapping("/update-question/{_id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> updateQuestion(
            @PathVariable("_id") String id,
            @RequestBody Map<String, Object> body
    ) {
        try {
            Map<String, Object> updated = questionService.updateQuestionSv(id, body);

            return ResponseEntity.ok(
                    Map.of(
                            "status", 200,
                            "message", "Cập nhật thành công",
                            "data", updated
                    )
            );
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    Map.of("status", 400, "message", e.getMessage())
            );
        }
    }

    // Xóa câu hỏi
    @DeleteMapping("/delete-question/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> deleteQuestion(
            @PathVariable("id") String id
    ) {
        try {
            Map<String, Object> result = questionService.deleteQuestionSv(id);

            return ResponseEntity.ok(
                    Map.of(
                            "status", 200,
                            "message", "Xóa thành công",
                            "data", result
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


}
