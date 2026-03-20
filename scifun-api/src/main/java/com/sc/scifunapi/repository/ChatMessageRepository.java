package com.sc.scifunapi.repository;

import com.sc.scifunapi.entity.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ChatMessageRepository extends MongoRepository<ChatMessage, String> {
    Page<ChatMessage> findByConversationId(String conversationId, Pageable pageable);
    List<ChatMessage> findByConversationIdOrderByCreatedAtAsc(String conversationId);

    // Thêm: chỉ lấy N tin nhắn gần nhất để tránh vượt context window
    List<ChatMessage> findTop20ByConversationIdOrderByCreatedAtAsc(String conversationId);
}
