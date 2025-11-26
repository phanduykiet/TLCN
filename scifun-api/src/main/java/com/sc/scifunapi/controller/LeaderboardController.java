package com.sc.scifunapi.controller;

import com.sc.scifunapi.service.LeaderboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/leaderboards")
@RequiredArgsConstructor
public class LeaderboardController {

    private final LeaderboardService leaderboardService;

    // POST /leaderboards/rebuild/{subjectId}?period=alltime|daily|weekly|monthly
    @PostMapping("/rebuild/{subjectId}")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<Map<String, Object>> rebuildSubjectLeaderboard(
            @PathVariable String subjectId,
            @RequestParam(name = "period", required = false, defaultValue = "alltime") String period
    ) {
        try {
            if (subjectId == null || subjectId.isBlank()) {
                return ResponseEntity.badRequest().body(
                        Map.of(
                                "status", 400,
                                "message", "subjectId is required"
                        )
                );
            }

            Map<String, Object> result = leaderboardService.rebuildSubjectLeaderboard(subjectId, period);

            return ResponseEntity.ok(
                    Map.of(
                            "status", 200,
                            "message", "Rebuild leaderboard thành công",
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

    // Lấy bảng xếp hạng cho một môn học
    @GetMapping("/list/{subjectId}")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<Map<String, Object>> getSubjectLeaderboard(
            @PathVariable String subjectId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false, defaultValue = "alltime") String period
    ) {
        try {
            Map<String, Object> result =
                    leaderboardService.getSubjectLeaderboardSv(subjectId, page, limit, period);

            return ResponseEntity.ok(
                    Map.of(
                            "status", 200,
                            "total", result.get("total"),
                            "page", result.get("page"),
                            "limit", result.get("limit"),
                            "data", result.get("data")
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
