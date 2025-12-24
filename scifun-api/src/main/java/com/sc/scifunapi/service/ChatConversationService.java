package com.sc.scifunapi.service;

import com.sc.scifunapi.entity.ChatConversation;
import com.sc.scifunapi.repository.ChatConversationRepository;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
public class ChatConversationService {

    private final ChatConversationRepository conversationRepo;

    public ChatConversationService(ChatConversationRepository conversationRepo) {
        this.conversationRepo = conversationRepo;
    }

    public ChatConversation getOrCreateOpenConversation(String userId) {

        return conversationRepo
                .findFirstByUserIdAndStatusOrderByUpdatedAtDesc(userId, "OPEN")
                .orElseGet(() -> {
                    ChatConversation convo = ChatConversation.builder()
                            .id(UUID.randomUUID().toString())   // ⭐ ID ngẫu nhiên
                            .userId(userId)
                            .status("OPEN")
                            .createdAt(new Date())
                            .updatedAt(new Date())
                            .build();
                    return conversationRepo.save(convo);
                });
    }

    public List<ChatConversation> findByUser(String userId) {
        return conversationRepo.findByUserIdOrderByUpdatedAtDesc(userId);
    }

    public List<ChatConversation> findAll() {
        return conversationRepo.findAll(Sort.by(Sort.Direction.DESC, "updatedAt"));
    }

    public ChatConversation getConversation(String conversationId, Authentication auth) {
        ChatConversation convo = conversationRepo.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found"));

        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        // USER chỉ được xem conversation của chính mình
        if (!isAdmin) {
            String userId = (String) SecurityContextHolder.getContext().getAuthentication().getDetails();
            if (convo.getUserId() == null || !convo.getUserId().equals(userId)) {
                throw new AccessDeniedException("Không có quyền truy cập room này");
            }
        }

        return convo;
    }

}
