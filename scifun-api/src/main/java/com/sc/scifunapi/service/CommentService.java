package com.sc.scifunapi.service;

import com.sc.scifunapi.dto.comment.CommentDto;
import com.sc.scifunapi.dto.comment.PagedCommentsResponse;
import com.sc.scifunapi.entity.Comment;
import com.sc.scifunapi.entity.Notification;
import com.sc.scifunapi.entity.User;
import com.sc.scifunapi.enums.NotificationType;
import com.sc.scifunapi.repository.NotificationRepository;
import com.sc.scifunapi.repository.UserRepository;
import com.sc.scifunapi.repository.CommentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class CommentService {

    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public CommentService(CommentRepository commentRepository, UserRepository userRepository, NotificationRepository notificationRepository,
                          SimpMessagingTemplate messagingTemplate) {
        this.commentRepository = commentRepository;
        this.userRepository = userRepository;
        this.notificationRepository = notificationRepository;
        this.messagingTemplate = messagingTemplate;
    }

    public PagedCommentsResponse listRootComments(int page, int limit) {
        int safePage = Math.max(page, 1);
        int safeLimit = Math.min(Math.max(limit, 1), 100);

        PageRequest pageable = PageRequest.of(
                safePage - 1,
                safeLimit,
                Sort.by(Sort.Direction.DESC, "createdAt") // root: mới -> cũ
        );

        Page<Comment> result = commentRepository.findByParentIdIsNull(pageable);

        long total = result.getTotalElements();
        int totalPages = result.getTotalPages();
        boolean hasMore = safePage * safeLimit < total;

        return new PagedCommentsResponse(
                null,
                result.getContent(),
                safePage,
                safeLimit,
                total,
                totalPages,
                hasMore
        );
    }

    public PagedCommentsResponse listReplies(String parentId, int page, int limit) {
        int safePage = Math.max(page, 1);
        int safeLimit = Math.min(Math.max(limit, 1), 100);

        // (tuỳ chọn) kiểm tra tồn tại comment cha
        commentRepository.findById(parentId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy comment cha"));

        PageRequest pageable = PageRequest.of(
                safePage - 1,
                safeLimit,
                Sort.by(Sort.Direction.ASC, "createdAt") // replies: cũ -> mới
        );

        Page<Comment> result = commentRepository.findByParentId(parentId, pageable);

        long total = result.getTotalElements();
        int totalPages = result.getTotalPages();
        boolean hasMore = safePage * safeLimit < total;

        return new PagedCommentsResponse(
                parentId,
                result.getContent(),
                safePage,
                safeLimit,
                total,
                totalPages,
                hasMore
        );
    }

    public Comment getCommentDetail(String id) {
        return commentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy comment"));
    }

    @Transactional
    public CommentDto createComment(String userId, String content, String parentId) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Nội dung bình luận không được trống");
        }

        // 1) Nếu là reply: lấy parent để:
        // - tăng repliesCount
        // - xác định user bị reply
        Comment parent = null;
        String repliedUserId = null;

        if (parentId != null && !parentId.isBlank()) {
            parent = commentRepository.findById(parentId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy bình luận cha (parentId)"));

            repliedUserId = parent.getUserId();

            // tăng repliesCount cho comment cha
            parent.setRepliesCount(parent.getRepliesCount() + 1);
            parent.setUpdatedAt(new Date());
            commentRepository.save(parent);
        }

        var u = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));
        // 2) Tạo comment mới
        Date now = new Date();
        Comment newComment = Comment.builder()
                .userId(userId)
                .userName(u.getFullname())
                .userAvatar(u.getAvatar())
                .content(content.trim())
                .parentId((parentId != null && !parentId.isBlank()) ? parentId : null)
                .repliesCount(0)
                .createdAt(now)
                .updatedAt(now)
                .build();

        Comment saved = commentRepository.save(newComment);

        // Nếu là reply và không tự reply chính mình -> tạo notification + push realtime
        if (repliedUserId != null
                && !repliedUserId.isBlank()
                && !repliedUserId.equals(userId)) {

            String preview = saved.getContent();
            if (preview != null && preview.length() > 80) {
                preview = preview.substring(0, 80) + "...";
            }

            Map<String, Object> data = new HashMap<>();
            data.put("type", "comment_reply");
            data.put("commentId", saved.getId());
            data.put("parentId", saved.getParentId());
            data.put("fromUserId", userId);

            Notification noti = Notification.builder()
                    .userId(repliedUserId)
                    .type(NotificationType.COMMENT_REPLY)   // đảm bảo enum có COMMENT_REPLY
                    .title("Bạn có phản hồi mới")
                    .message(preview != null ? preview : "Có người đã phản hồi bình luận của bạn.")
                    .data(data)
                    .link("/comments/" + saved.getId())      // em đổi theo routing app
                    .isRead(false)
                    .createdAt(new Date())
                    .build();

            Notification savedNoti = notificationRepository.save(noti);

            // Push realtime đến đúng user (giống cách em làm comment reply)
            messagingTemplate.convertAndSendToUser(
                    savedNoti.getUserId(),
                    "/queue/notifications",
                    savedNoti
            );
        }


        return toDto(saved, repliedUserId);
    }

    private CommentDto toDto(Comment c, String repliedUserId) {
        return new CommentDto(
                c.getId(),
                c.getUserId(),
                c.getUserName(),
                c.getUserAvatar(),
                c.getContent(),
                c.getParentId(),
                c.getRepliesCount(),
                c.getCreatedAt(),
                c.getUpdatedAt(),
                repliedUserId
        );
    }
}

