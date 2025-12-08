// src/main/java/com/sc/scifunapi/controller/SubjectController.java
package com.sc.scifunapi.controller;

import com.sc.scifunapi.entity.Subject;
import com.sc.scifunapi.service.SubjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/subject")
public class SubjectController {

    private final SubjectService subjectService;


    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/sync-all-subjects-es")
    public ResponseEntity<?> syncAllSubjectsToES() {
        try {
            subjectService.reindexAllSubjects();
            return ResponseEntity.ok(Map.of(
                    "status", 200,
                    "message", "Đồng bộ lại toàn bộ Subject lên Elasticsearch thành công"
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.status(500).body(Map.of(
                    "status", 500,
                    "message", "Lỗi khi đồng bộ Elasticsearch: " + e.getMessage()
            ));
        }
    }


    // Tạo môn học
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(value = "/create-subject", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createSubject(
            @RequestParam Map<String, String> form,
            @RequestPart(value = "image", required = false) MultipartFile image
    ) {
        try {
            Subject subject = subjectService.createSubjectSv(form, image);
            return ResponseEntity.ok(Map.of(
                    "status", 200,
                    "message", "Tạo môn học thành công",
                    "data", subject
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", 400,
                    "message", "Error creating subject" + e.getMessage()
            ));
        }
    }

    // Lấy chi tiết môn học
    @GetMapping("/get-subjectById/{id}")
    public ResponseEntity<Map<String, Object>> getSubjectById(@PathVariable("id") String id) {
        try {
            var subject = subjectService.getSubjectByIdSv(id);
            return ResponseEntity.ok(Map.of(
                    "status", 200,
                    "message", "Lấy chi tiết môn học thành công",
                    "data", subject
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", 400,
                    "message", e.getMessage()
            ));
        }
    }

    // Cập nhật môn học
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping(value = "/update-subject/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateSubject(
            @PathVariable("id") String id,
            @RequestParam Map<String, String> form,
            @RequestPart(value = "image", required = false) MultipartFile image
    ) {
        try {
            var subject = subjectService.updateSubjectSv(id, form, image);
            return ResponseEntity.ok(Map.of(
                    "status", 200,
                    "message", "Cập nhật môn học thành công",
                    "data", subject
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", 400,
                    "message", e.getMessage()
            ));
        }
    }

    // Xóa môn học
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/delete-subject/{id}")
    public ResponseEntity<?> deleteSubject(@PathVariable("id") String id) {
        try {
            subjectService.deleteSubjectSv(id);
            return ResponseEntity.ok(Map.of(
                    "status", 200,
                    "message", "Xóa môn học thành công"
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", 400,
                    "message", e.getMessage()
            ));
        }
    }

    // Lấy danh sách môn học (phân trang + tìm kiếm)
    @GetMapping("/get-subjects")
    public ResponseEntity<?> getSubjects(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String search
    ) {
        try {
            Map<String, Object> result = subjectService.getSubjectsSv(page, limit, search);
            return ResponseEntity.ok(Map.of(
                    "status", 200,
                    "message", "Lấy danh sách môn học thành công",
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
