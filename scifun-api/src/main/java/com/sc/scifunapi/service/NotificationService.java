package com.sc.scifunapi.service;

import com.sc.scifunapi.entity.Notification;
import com.sc.scifunapi.entity.User;
import com.sc.scifunapi.enums.NotificationType;
import com.sc.scifunapi.repository.NotificationRepository;
import com.sc.scifunapi.dto.notification.RankChangedParams;
import com.sc.scifunapi.repository.UserRepository;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final MailService emailService;
    private final SimpMessagingTemplate messagingTemplate;

    @Value("${client.url:http://localhost:3000}")
    private String clientUrl;

    public Map<String, Object> getNotifications(String userId, int page, int limit) {

        if (page <= 0) page = 1;
        if (limit <= 0) limit = 20;

        int skip = (page - 1) * limit;

        List<Notification> list = notificationRepository
                .findByUserIdOrderByCreatedAtDesc(userId, skip, limit);

        long total = notificationRepository.countByUserId(userId);

        return Map.of(
                "items", list,
                "pagination", Map.of(
                        "page", page,
                        "limit", limit,
                        "total", total,
                        "totalPages", (int) Math.ceil((double) total / limit)
                )
        );
    }

    public Notification markAsRead(String notificationId, String userId) {
        Notification noti = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Thông báo không tồn tại"));

        // Chỉ cho phép owner được sửa (hoặc em có thể cho ADMIN override luôn)
        if (!noti.getUserId().equals(userId)) {
            throw new RuntimeException("Bạn không có quyền cập nhật thông báo này");
        }

        if (!noti.isRead()) {
            noti.setRead(true);
            noti = notificationRepository.save(noti);
        }

        return noti;
    }

    /**
     * Thông báo khi thứ hạng thay đổi (tương đương notifyRankChanged bên TS)
     */
    public void notifyRankChanged(RankChangedParams params) {
        String userId      = params.getUserId();
        String subjectId   = params.getSubjectId();
        String subjectName = params.getSubjectName();
        String period      = params.getPeriod();
        int oldRank        = params.getOldRank();
        int newRank        = params.getNewRank();
        boolean persist    = params.isPersist();
        boolean email      = params.isEmail();

        // Không đổi hạng thì thôi
        if (oldRank == newRank) {
            return;
        }

        String change = newRank < oldRank ? "up" : "down";
        int diff = Math.abs(newRank - oldRank);

        // 1) Lưu DB
        if (persist) {
            Notification notification = Notification.builder()
                    .userId(userId)
                    .type(NotificationType.RANK_CHANGED)
                    .title("Thay đổi xếp hạng")
                    .message(
                            "up".equals(change)
                                    ? String.format("Bạn đã tăng %d hạng (từ #%d → #%d)", diff, oldRank, newRank)
                                    : String.format("Bạn đã giảm %d hạng (từ #%d → #%d)", diff, oldRank, newRank)
                    )
                    .data(Map.of(
                            "subjectId", subjectId,
                            "subjectName", subjectName,
                            "period", period,
                            "oldRank", oldRank,
                            "newRank", newRank,
                            "change", change
                    ))
                    .link("/leaderboard")
                    .isRead(false)
                    .createdAt(new Date())
                    .build();

            Notification saved = notificationRepository.save(notification);

            // 2) realtime duy nhất
            messagingTemplate.convertAndSendToUser(
                    userId,
                    "/queue/notifications",
                    saved
            );
        }

        // 3) Gửi email text
        if (email) {
            Optional<User> optUser = userRepository.findById(userId);
            if (optUser.isPresent()) {
                User user = optUser.get();
                if (user.getEmail() != null && !user.getEmail().isBlank()) {
                    String subjectMail = String.format(
                            "[Quiz App] Thứ hạng của bạn đã %s",
                            "up".equals(change) ? "tăng" : "giảm"
                    );

                    String body = String.join("\n",
                            "Xin chào " + (user.getEmail() != null ? user.getEmail() : "bạn") + ",",
                            String.format(
                                    "Thứ hạng môn %s (%s) của bạn đã %s %d bậc: #%d → #%d.",
                                    subjectName,
                                    period,
                                    "up".equals(change) ? "tăng" : "giảm",
                                    diff,
                                    oldRank,
                                    newRank
                            ),
                            "Xem bảng xếp hạng: " + clientUrl + "/leaderboard"
                    );

                    emailService.sendMail(user.getEmail(), subjectMail, body);
                }
            }
        }
    }

    // ==== DTO cho params ====

    @Data
    @Builder
    @AllArgsConstructor
    public static class RankChangeParams {
        private String userId;
        private String subjectId;
        private String subjectName;
        private String period; // daily | weekly | monthly | alltime
        private int oldRank;
        private int newRank;
        private Boolean persist; // nullable -> default true
        private Boolean email;   // nullable -> default true
    }

    @Data
    @Builder
    @AllArgsConstructor
    public static class RankChangePayload {
        private String subjectId;
        private String subjectName;
        private String period;
        private int oldRank;
        private int newRank;
        private String change; // "up" | "down"
    }

    @Data
    @Builder
    @AllArgsConstructor
    public static class CommentReplyParams {
        private String targetUserId;
        private String fromUserName;
        private String content;
        private String commentId;
        private String parentId;
        private Boolean persist; // default true
        private Boolean email;   // default false
    }
}
