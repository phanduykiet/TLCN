package com.sc.scifunapi.controller;

import com.sc.scifunapi.entity.UserProgress;
import com.sc.scifunapi.service.UserProgressService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class UserProgressController {

    private final UserProgressService userProgressService;

    // Lấy tiến độ của 1 subject
    @GetMapping("/user-progress/{subjectId}")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<Map<String, Object>> getUserProgress(
            @PathVariable String subjectId
    ) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || auth.getName() == null) {
                return ResponseEntity.status(401).body(
                        Map.of(
                                "status", 401,
                                "message", "Không tìm thấy thông tin người dùng từ token"
                        )
                );
            }

            String userId = (String) auth.getDetails(); // trong JwtAuthFilter em set username = userId

            UserProgress progress = userProgressService.getUserProgressSv(userId, subjectId);

            return ResponseEntity.ok(
                    Map.of(
                            "status", 200,
                            "message", "Lấy tiến độ học tập thành công",
                            "data", progress
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
