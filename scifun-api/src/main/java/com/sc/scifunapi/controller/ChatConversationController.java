package com.sc.scifunapi.controller;

import com.sc.scifunapi.entity.ChatConversation;
import com.sc.scifunapi.entity.ChatMessage;
import com.sc.scifunapi.repository.ChatMessageRepository;
import com.sc.scifunapi.service.ChatConversationService;
import com.sc.scifunapi.service.ChatMessageService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatConversationController {

    private final ChatConversationService conversationService;
    private final ChatMessageService messageService;

    public ChatConversationController(ChatConversationService conversationService, ChatMessageService messageService) {
        this.conversationService = conversationService;
        this.messageService = messageService;
    }

    @PostMapping("/conversation")
    public ResponseEntity<?> openChat(
            @RequestParam(defaultValue = "HUMAN") String type) {

        String userId = (String) SecurityContextHolder.getContext()
                .getAuthentication().getDetails();

        // Chỉ USER mới được tạo room AI
        if ("AI".equals(type)) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            boolean isUser = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_USER"));
            if (!isUser) {
                return ResponseEntity.status(403).body(Map.of("message", "Chỉ USER mới chat được với AI"));
            }
        }

        ChatConversation convo = conversationService.getOrCreateOpenConversation(userId, type);

        return ResponseEntity.ok(Map.of(
                "conversationId", convo.getId(),
                "status", convo.getStatus(),
                "type", convo.getType()
        ));
    }

    @GetMapping("/conversations")
    public ResponseEntity<?> listConversations(
            Authentication auth,
            @RequestParam(required = false) String type) {

        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        String userId = (String) SecurityContextHolder.getContext()
                .getAuthentication().getDetails();

        List<ChatConversation> items;

        if (isAdmin) {
            // Admin chỉ thấy HUMAN room
            items = conversationService.findAllByType("HUMAN");
        } else {
            // User lọc theo type nếu có, không thì lấy tất cả
            items = (type != null)
                    ? conversationService.findByUserAndType(userId, type)
                    : conversationService.findByUser(userId);
        }

        return ResponseEntity.ok(items);
    }

    @GetMapping("/{conversationId}/messages")
    public ResponseEntity<?> getMessages(
            @PathVariable String conversationId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit,
            Authentication auth
    ) {
        ChatConversation convo = conversationService.getConversation(conversationId, auth);

        Page<ChatMessage> messages = messageService.getMessages(conversationId, page, limit);

        return ResponseEntity.ok(Map.of(
                "conversationId", conversationId,
                "items", messages.getContent(),
                "page", page,
                "total", messages.getTotalElements()
        ));
    }


}
