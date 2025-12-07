package com.sc.scifunapi.controller;

import com.sc.scifunapi.service.TopicService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/topic")
public class TopicController {

    private final TopicService topicService;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/sync-all-topics-es")
    public ResponseEntity<?> syncAllTopicsToES() {
        try {
            topicService.reindexAllTopics();
            return ResponseEntity.ok(Map.of(
                    "status", 200,
                    "message", "Đồng bộ lại toàn bộ Topic lên Elasticsearch thành công"
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.status(500).body(Map.of(
                    "status", 500,
                    "message", "Lỗi khi đồng bộ Elasticsearch: " + e.getMessage()
            ));
        }
    }


    // Tạo chủ đề (ADMIN)
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/create-topic")
    public ResponseEntity<?> createTopic(@RequestBody Map<String, Object> body) {
        try {
            var topic = topicService.createTopicSv(body);
            return ResponseEntity.ok(Map.of(
                    "status", 200,
                    "message", "Tạo chủ đề thành công",
                    "data", topic
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", 400,
                    "message", e.getMessage()
            ));
        }
    }

    // Lấy danh sách phân trang tìm kiếm
    @GetMapping("/get-topics")
    public ResponseEntity<Map<String, Object>> getTopics(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String subjectId,
            @RequestParam(required = false) String search
    ) {
        try {
            Map<String, Object> data = topicService.getTopics(page, limit, subjectId, search);

            return ResponseEntity.ok(Map.of(
                    "status", 200,
                    "message", "Lấy danh sách chủ đề thành công",
                    "data", data
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", 400,
                    "message", e.getMessage()
            ));
        }
    }

    // Lấy chi tiết chủ đề
    @GetMapping("/get-topicById/{_id}")
    public ResponseEntity<Map<String, Object>> getTopicById(
            @PathVariable("_id") String id
    ) {
        try {
            Map<String, Object> topic = topicService.getTopicById(id);
            return ResponseEntity.ok(Map.of(
                    "status", 200,
                    "message", "Lấy chi tiết chủ đề thành công",
                    "data", topic
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", 400,
                    "message", e.getMessage()
            ));
        }
    }

    // Cập nhật chủ đề
    @PutMapping("/update-topic/{_id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> updateTopic(
            @PathVariable("_id") String id,
            @RequestBody Map<String, Object> body
    ) {
        try {
            Map<String, Object> topic = topicService.updateTopic(id, body);
            return ResponseEntity.ok(Map.of(
                    "status", 200,
                    "message", "Cập nhật chủ đề thành công",
                    "data", topic
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", 400,
                    "message", e.getMessage()
            ));
        }
    }

    // Xóa chủ đề
    @DeleteMapping("/delete-topic/{_id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> deleteTopic(
            @PathVariable("_id") String id
    ) {
        try {
            topicService.deleteTopic(id);
            return ResponseEntity.ok(Map.of(
                    "status", 200,
                    "message", "Xóa chủ đề thành công"
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", 400,
                    "message", e.getMessage()
            ));
        }
    }

}
