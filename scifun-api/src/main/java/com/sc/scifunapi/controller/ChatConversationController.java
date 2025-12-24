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
    public ResponseEntity<?> openChat(Principal principal) {
        String userId = (String) SecurityContextHolder.getContext().getAuthentication().getDetails();

        ChatConversation convo = conversationService.getOrCreateOpenConversation(userId);

        return ResponseEntity.ok(Map.of(
                "conversationId", convo.getId(),
                "status", convo.getStatus()
        ));
    }

    @GetMapping("/conversations")
    public ResponseEntity<?> listConversations(Authentication auth) {

        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        String userId = (String) SecurityContextHolder.getContext().getAuthentication().getDetails();

        List<ChatConversation> items = isAdmin
                ? conversationService.findAll()
                : conversationService.findByUser(userId);

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
