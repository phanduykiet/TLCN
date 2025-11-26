package com.sc.scifunapi.controller;

import com.sc.scifunapi.service.QuizSubmissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/submission")
@RequiredArgsConstructor
public class QuizSubmissionController {

    private final QuizSubmissionService quizSubmissionService;

    // Nộp bài + chấm điểm
    @PostMapping("/handle-submit")
    public ResponseEntity<Map<String, Object>> handleSubmitQuiz(
            @RequestBody Map<String, Object> payload
    ) {
        try {
            Map<String, Object> result = quizSubmissionService.handleSubmitQuizSv(payload);
            return ResponseEntity.ok(
                    Map.of(
                            "status", 200,
                            "message", "Thành công",
                            "data", result
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

    // Xem chi tiết bài làm + giải thích chi tiết
    @GetMapping("/get-submissionDetail/{submissionId}")
    public ResponseEntity<Map<String, Object>> getSubmissionDetail(
            @PathVariable String submissionId
    ) {
        try {
            Map<String, Object> result = quizSubmissionService.getSubmissionDetailSv(submissionId);
            return ResponseEntity.ok(
                    Map.of(
                            "status", 200,
                            "message", "Thành công",
                            "data", result
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

    // Lấy danh sách kết quả (Result) với phân trang
    @GetMapping("/get-all")
    public ResponseEntity<Map<String, Object>> getResults(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer limit
    ) {
        try {
            int pageNumber = (page != null && page > 0) ? page : 1;
            int pageSize   = (limit != null && limit > 0) ? limit : 10;

            Map<String, Object> result = quizSubmissionService.getResultsSv(pageNumber, pageSize);

            return ResponseEntity.ok(
                    Map.of(
                            "status", 200,
                            "message", "Lấy danh sách thành công",
                            "data", result
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

}
