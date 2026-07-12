package com.sc.scifunapi.controller;

import com.sc.scifunapi.dto.userProgress.ProgressStatDTO;
import com.sc.scifunapi.dto.userProgress.ProgressStatsOverviewDTO;
import com.sc.scifunapi.entity.UserProgress;
import com.sc.scifunapi.enums.StatPeriod;
import com.sc.scifunapi.service.UserProgressService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
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
                        @PathVariable String subjectId) {
                try {
                        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                        if (auth == null || auth.getName() == null) {
                                return ResponseEntity.status(401).body(
                                                Map.of(
                                                                "status", 401,
                                                                "message",
                                                                "Không tìm thấy thông tin người dùng từ token"));
                        }

                        String userId = (String) auth.getDetails();

                        UserProgress progress = userProgressService.getUserProgressSv(userId, subjectId);

                        return ResponseEntity.ok(
                                        Map.of(
                                                        "status", 200,
                                                        "message", "Lấy tiến độ học tập thành công",
                                                        "data", progress));
                } catch (RuntimeException e) {
                        return ResponseEntity.badRequest().body(
                                        Map.of(
                                                        "status", 400,
                                                        "message", e.getMessage()));
                }
        }

        @GetMapping("/progress-stats")
        @PreAuthorize("hasAnyRole('ADMIN','USER')")
        public ResponseEntity<Map<String, Object>> getProgressStats() {
        try {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                if (auth == null || auth.getName() == null) {
                return ResponseEntity.status(401).body(
                        Map.of(
                                "status", 401,
                                "message",
                                "Không tìm thấy thông tin người dùng từ token"));
                }

                String userId = (String) auth.getDetails();

                ProgressStatsOverviewDTO stats = userProgressService.getProgressStatsSv(userId);

                return ResponseEntity.ok(
                        Map.of(
                                "status", 200,
                                "message", "Lấy thống kê tiến độ thành công",
                                "data", stats));
        } catch (RuntimeException e) {
                return ResponseEntity.badRequest().body(
                        Map.of(
                                "status", 400,
                                "message", e.getMessage()));
        }
        }
}
