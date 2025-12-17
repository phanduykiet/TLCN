package com.sc.scifunapi.controller;

import com.sc.scifunapi.dto.comment.CommentDto;
import com.sc.scifunapi.dto.comment.NewCommentRequest;
import com.sc.scifunapi.service.CommentService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

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
}

