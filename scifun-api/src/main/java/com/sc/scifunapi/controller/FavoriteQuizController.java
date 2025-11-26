package com.sc.scifunapi.controller;

import com.sc.scifunapi.entity.FavoriteQuiz;
import com.sc.scifunapi.service.FavoriteQuizService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/favorite-quiz")
@RequiredArgsConstructor
public class FavoriteQuizController {

    private final FavoriteQuizService favoriteQuizService;

    // Thêm vào yêu thích
    @PostMapping("/add")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<Map<String, Object>> addFavoriteQuiz(
            @RequestBody Map<String, Object> body,
            Authentication authentication
    ) {
        try {
            String quizId = body.get("quizId") != null ? body.get("quizId").toString() : null;

            // Lấy userId từ token
            if (authentication == null || authentication.getDetails() == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "status", 400,
                        "message", "Không xác định được người dùng (authentication null)"
                ));
            }
            String userId = (String) authentication.getDetails(); // đã cấu hình JwtAuthFilter để set principal = userId

            if (userId == null || userId.isBlank() || quizId == null || quizId.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "status", 400,
                        "message", "userId và quizId là bắt buộc"
                ));
            }

            FavoriteQuiz favorite = favoriteQuizService.addFavoriteQuiz(userId, quizId);

            return ResponseEntity.ok(Map.of(
                    "status", 200,
                    "message", "Đã thêm vào yêu thích",
                    "data", favorite
            ));

        } catch (DuplicateKeyException e) {
            // Trùng unique index (user + quiz)
            return ResponseEntity.badRequest().body(Map.of(
                    "status", 400,
                    "message", "Quiz đã có trong danh sách yêu thích"
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", 400,
                    "message", "Error adding favorite: " + e.getMessage()
            ));
        }
    }

    // Bỏ yêu thích
    @DeleteMapping("/remove/{quizId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<Map<String, Object>> removeFavoriteQuiz(
            @PathVariable String quizId,
            Authentication authentication
    ) {
        try {
            if (authentication == null || authentication.getDetails() == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "status", 400,
                        "message", "Không xác định được người dùng (authentication null)"
                ));
            }
            String userId = (String) authentication.getDetails(); // userId từ JWT

            if (userId == null || userId.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "status", 400,
                        "message", "userId là bắt buộc"
                ));
            }

            boolean removed = favoriteQuizService.removeFavoriteQuiz(userId, quizId);

            if (!removed) {
                return ResponseEntity.status(404).body(Map.of(
                        "status", 404,
                        "message", "Quiz không có trong danh sách yêu thích"
                ));
            }

            return ResponseEntity.ok(Map.of(
                    "status", 200,
                    "message", "Đã bỏ yêu thích"
            ));

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", 400,
                    "message", "Error removing favorite: " + e.getMessage()
            ));
        }
    }

    // Lấy danh sách quiz yêu thích
    @GetMapping("/list")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<Map<String, Object>> getFavoriteQuizzes(
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "10") Integer limit,
            @RequestParam(required = false) String topicId,
            Authentication authentication
    ) {
        try {
            if (authentication == null || authentication.getDetails() == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "status", 400,
                        "message", "Không xác định được người dùng (authentication null)"
                ));
            }

            String userId = (String) authentication.getDetails(); // userId từ JWT

            if (userId == null || userId.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "status", 400,
                        "message", "userId là bắt buộc"
                ));
            }

            Map<String, Object> favorites =
                    favoriteQuizService.getFavoriteQuizzes(userId, page, limit, topicId);

            return ResponseEntity.ok(Map.of(
                    "status", 200,
                    "message", "Lấy danh sách yêu thích thành công",
                    "data", favorites
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", 400,
                    "message", "Error getting favorites: " + e.getMessage()
            ));
        }
    }
}
