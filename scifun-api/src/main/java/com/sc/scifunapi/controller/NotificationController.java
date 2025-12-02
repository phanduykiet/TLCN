// src/main/java/com/sc/scifunapi/controller/NotificationController.java
package com.sc.scifunapi.controller;

import com.sc.scifunapi.entity.Notification;
import com.sc.scifunapi.repository.NotificationRepository;
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

    // GET /api/v1/notifications?page=&limit=
    @GetMapping("/notifications")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<?> getNotifications(
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "limit", defaultValue = "20") int limit
    ) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String userId = (auth != null && auth.getDetails() != null)
                    ? auth.getDetails().toString()
                    : null;

            if (userId == null || userId.isBlank()) {
                return ResponseEntity.status(401).body(
                        Map.of(
                                "status", 401,
                                "success", false,
                                "message", "Unauthorized"
                        )
                );
            }

            // Chuẩn hóa page / limit
            if (page < 1) page = 1;
            if (limit < 1) limit = 20;

            Pageable pageable = PageRequest.of(
                    page - 1,
                    limit,
                    Sort.by(Sort.Direction.DESC, "createdAt")
            );

            // Lấy dữ liệu song song: page + unreadCount
            Page<Notification> notificationPage =
                    notificationRepository.findByUserId(userId, pageable);
            long unreadCount =
                    notificationRepository.countByUserIdAndIsReadFalse(userId);

            List<Notification> items = notificationPage.getContent();
            long total = notificationPage.getTotalElements();
            int totalPages = notificationPage.getTotalPages();

            // Dùng HashMap để tránh NPE khi Map.of gặp value null
            Map<String, Object> data = new HashMap<>();
            data.put("notifications", items);
            data.put("total", total);
            data.put("unreadCount", unreadCount);
            data.put("page", page);
            data.put("totalPages", totalPages);

            Map<String, Object> response = new HashMap<>();
            response.put("status", 200);
            response.put("success", true);
            response.put("data", data);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(
                    Map.of(
                            "status", 400,
                            "success", false,
                            "message", e.getMessage()
                    )
            );
        }
    }

    @PostMapping("/mark-as-read/{notificationId}")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<?> markAsRead(@PathVariable String notificationId) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String userId = auth != null ? auth.getDetails().toString() : null;

            if (userId == null) {
                return ResponseEntity.status(401).body(
                        Map.of("status", 401, "success", false, "message", "Unauthorized")
                );
            }

            Notification doc = notificationRepository
                    .findByIdAndUserId(notificationId, userId)
                    .orElse(null);

            if (doc == null) {
                return ResponseEntity.status(404).body(
                        Map.of("status", 404, "success", false, "message", "Không tìm thấy")
                );
            }

            doc.setRead(true);
            notificationRepository.save(doc);

            return ResponseEntity.ok(
                    Map.of(
                            "status", 200,
                            "success", true,
                            "message", "Đã đánh dấu đọc",
                            "data", doc
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
