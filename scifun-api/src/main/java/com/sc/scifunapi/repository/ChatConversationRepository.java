package com.sc.scifunapi.repository;

import com.sc.scifunapi.entity.ChatConversation;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ChatConversationRepository extends MongoRepository<ChatConversation, String> {

    Optional<ChatConversation> findFirstByUserIdAndStatusOrderByUpdatedAtDesc(String userId, String status);

    List<ChatConversation> findByUserIdOrderByUpdatedAtDesc(String userId);
}

