package com.sc.scifunapi.controller;

import com.sc.scifunapi.dto.comment.CommentDto;
import com.sc.scifunapi.dto.comment.NewCommentRequest;
import com.sc.scifunapi.dto.comment.PagedCommentsResponse;
import com.sc.scifunapi.dto.common.ApiError;
import com.sc.scifunapi.dto.common.ApiResponse;
import com.sc.scifunapi.entity.Comment;
import com.sc.scifunapi.service.CommentService;
import com.sc.scifunapi.util.MongoIdUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;

@Controller
public class CommentWsController {

    private final CommentService commentService;
    private final SimpMessagingTemplate messagingTemplate;

    public CommentWsController(CommentService commentService, SimpMessagingTemplate messagingTemplate) {
        this.commentService = commentService;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/comment/new") // client gửi: /app/comment/new
    public void onNewComment(NewCommentRequest req, Principal principal) {
        String userId = principal.getName();

        CommentDto saved = commentService.createComment(userId, req.content(), req.parentId());

        messagingTemplate.convertAndSend("/topic/comment/new", saved);
    }

    // GET /api/comments/root?page=1&limit=10
    @GetMapping("/api/v1/comments")
    public ResponseEntity<?> listRootComments(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit
    ) {
        try {
            PagedCommentsResponse data = commentService.listRootComments(page, limit);
            return ResponseEntity.ok(new ApiResponse<>(200, true, data));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ApiError(500, false, e.getMessage()));
        }
    }

    // GET /api/comments/{parentId}/replies?page=1&limit=10
    @GetMapping("api/v1/comments/{parentId}/replies")
    public ResponseEntity<?> listReplies(
            @PathVariable String parentId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit
    ) {
        try {
            if (!MongoIdUtil.isValidObjectId(parentId)) {
                return ResponseEntity.badRequest().body(new ApiError(400, false, "parentId không hợp lệ"));
            }

            PagedCommentsResponse data = commentService.listReplies(parentId, page, limit);
            return ResponseEntity.ok(new ApiResponse<>(200, true, data));
        } catch (IllegalArgumentException e) {
            // parent không tồn tại hoặc comment không tồn tại
            String msg = e.getMessage();
            int code = msg != null && msg.contains("comment cha") ? 404 : 404;
            return ResponseEntity.status(code).body(new ApiError(code, false, msg));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ApiError(500, false, e.getMessage()));
        }
    }

    // GET /api/comments/{id}
    @GetMapping("/api/v1/comments/{id}")
    public ResponseEntity<?> getCommentDetail(@PathVariable String id) {
        try {
            if (!MongoIdUtil.isValidObjectId(id)) {
                return ResponseEntity.badRequest().body(new ApiError(400, false, "id không hợp lệ"));
            }

            Comment comment = commentService.getCommentDetail(id);
            return ResponseEntity.ok(new ApiResponse<>(200, true, comment));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(new ApiError(404, false, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ApiError(500, false, e.getMessage()));
        }
    }
}

