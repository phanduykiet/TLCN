package com.sc.scifunapi.service;

import com.sc.scifunapi.entity.ChatMessage;
import com.sc.scifunapi.repository.ChatMessageRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class ChatMessageService {

    private final ChatMessageRepository messageRepo;

    public ChatMessageService(ChatMessageRepository messageRepo) {
        this.messageRepo = messageRepo;
    }

    public Page<ChatMessage> getMessages(String conversationId, int page, int limit) {
        return messageRepo.findByConversationId(
                conversationId,
                PageRequest.of(page - 1, limit, Sort.by("createdAt").ascending())
        );
    }
}
