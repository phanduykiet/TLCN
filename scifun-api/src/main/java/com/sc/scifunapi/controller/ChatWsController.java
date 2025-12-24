package com.sc.scifunapi.controller;

import com.sc.scifunapi.dto.chat.SendChatMessageRequest;
import com.sc.scifunapi.service.ChatService;
import org.springframework.messaging.handler.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
public class ChatWsController {

    private final ChatService chatService;

    public ChatWsController(ChatService chatService) {
        this.chatService = chatService;
    }


    @MessageMapping("/chat/{conversationId}/send")
    public void send(@DestinationVariable String conversationId,
                     SendChatMessageRequest req,
                     Principal principal) {

        String senderId = principal.getName();

        String senderRole = resolveRole(principal);

        System.out.println("[WS CHAT] convo=" + conversationId
                + " senderId=" + senderId
                + " role=" + senderRole
                + " content=" + (req != null ? req.content() : null));

        chatService.sendMessage(conversationId, senderId, senderRole, req.content());
    }

    private String resolveRole(Principal principal) {
        if (principal instanceof Authentication auth) {
            for (GrantedAuthority ga : auth.getAuthorities()) {
                String a = ga.getAuthority();
                if ("ROLE_ADMIN".equals(a) || "ADMIN".equals(a)) return "ADMIN";
            }
        }
        return "USER";
    }


}
