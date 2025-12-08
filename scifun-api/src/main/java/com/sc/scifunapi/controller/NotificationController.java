// src/main/java/com/sc/scifunapi/controller/NotificationController.java
package com.sc.scifunapi.controller;

import com.sc.scifunapi.entity.Notification;
import com.sc.scifunapi.repository.NotificationRepository;
import com.sc.scifunapi.service.NotificationService;
import com.sc.scifunapi.service.TopicService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;

    // GET /api/v1/notifications?page=&limit=
    @GetMapping("/notifications")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<?> getNotifications(
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "limit", defaultValue = "10") int limit,
            Authentication authentication
    ) {
        try {
            String userId = (String) authentication.getDetails(); // lấy userId từ token

            Map<String, Object> result = notificationService.getNotifications(userId, page, limit);

            return ResponseEntity.ok(Map.of(
                    "status", 200,
                    "message", "Lấy danh sách thông báo thành công",
                    "data", result
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", 400,
                    "message", e.getMessage()
            ));
        }
    }


    @PostMapping("/mark-as-read/{notificationId}")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<?> markAsRead(
            @PathVariable String notificationId,
            Authentication authentication
    ) {
        try {
            String userId = (String) authentication.getDetails(); // lấy user từ token

            Notification updated = notificationService.markAsRead(notificationId, userId);

            return ResponseEntity.ok(Map.of(
                    "status", 200,
                    "message", "Đánh dấu thông báo đã đọc thành công",
                    "data", updated
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", 400,
                    "message", e.getMessage()
            ));
        }
    }


    // PATCH /api/v1/notifications/mark-all-as-read
    @PostMapping("/mark-all-as-read")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<?> markAllAsRead() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String userId = auth != null ? auth.getDetails().toString() : null;

            if (userId == null) {
                return ResponseEntity.status(401).body(
                        Map.of("status", 401, "success", false, "message", "Unauthorized")
                );
            }

            // Lấy tất cả thông báo chưa đọc của user
            List<Notification> unreadList =
                    notificationRepository.findByUserIdAndIsReadFalse(userId);

            if (!unreadList.isEmpty()) {
                unreadList.forEach(n -> n.setRead(true)); // isRead = true
                notificationRepository.saveAll(unreadList);
            }

            return ResponseEntity.ok(
                    Map.of(
                            "status", 200,
                            "success", true,
                            "message", "Đã đánh dấu tất cả đã đọc"
                    )
            );

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    Map.of(
                            "status", 400,
                            "success", false,
                            "message", e.getMessage()
                    )
            );
        }
    }
}
