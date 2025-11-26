package com.sc.scifunapi.controller;

import com.sc.scifunapi.service.VideoLessonService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/video-lesson")
@RequiredArgsConstructor
public class VideoLessonController {

    private final VideoLessonService videoLessonService;

    // Tạo video lesson
    @PostMapping("/create")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> createVideoLesson(
            @RequestBody Map<String, Object> body
    ) {
        try {
            Map<String, Object> videoLesson = videoLessonService.createVideoLessonSv(body);

            return ResponseEntity.ok(
                    Map.of(
                            "status", 200,
                            "message", "Tạo video lesson thành công",
                            "data", videoLesson
                    )
            );
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    Map.of(
                            "status", 400,
                            "message", "Error creating video lesson: " + e.getMessage()
                    )
            );
        }
    }

    // Cập nhật video lesson
    @PutMapping("/update/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> updateVideoLesson(
            @PathVariable String id,
            @RequestBody Map<String, Object> body
    ) {
        try {
            Map<String, Object> videoLesson = videoLessonService.updateVideoLessonSv(id, body);

            return ResponseEntity.ok(
                    Map.of(
                            "status", 200,
                            "message", "Cập nhật video lesson thành công",
                            "data", videoLesson
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

    // Xóa video lesson
    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> deleteVideoLesson(
            @PathVariable String id
    ) {
        try {
            Map<String, Object> result = videoLessonService.deleteVideoLessonSv(id);

            return ResponseEntity.ok(
                    Map.of(
                            "status", 200,
                            "message", result.get("message"),
                            "data", result.get("videoLesson")
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

    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> getVideoLessons(
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "limit", defaultValue = "10") int limit,
            @RequestParam(name = "topicId", required = false) String topicId
    ) {
        try {
            Map<String, Object> result = videoLessonService.getVideoLessonsSv(page, limit, topicId);

            return ResponseEntity.ok(
                    Map.of(
                            "status", 200,
                            "message", "Lấy danh sách video lessons thành công",
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

    // Lấy chi tiết video lesson
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getVideoLessonById(
            @PathVariable("id") String id
    ) {
        try {
            Map<String, Object> videoLesson = videoLessonService.getVideoLessonByIdSv(id);

            return ResponseEntity.ok(
                    Map.of(
                            "status", 200,
                            "message", "Lấy chi tiết video lesson thành công",
                            "data", videoLesson
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
