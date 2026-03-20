package com.sc.scifunapi.repository;

import com.sc.scifunapi.entity.ChatConversation;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ChatConversationRepository extends MongoRepository<ChatConversation, String> {

    Optional<ChatConversation> findFirstByUserIdAndStatusOrderByUpdatedAtDesc(String userId, String status);

    List<ChatConversation> findByUserIdOrderByUpdatedAtDesc(String userId);
    // Đã có sẵn cho HUMAN chat
    Optional<ChatConversation> findByUserIdAndAdminId(String userId, String adminId);

    // Thêm cho AI chat — mỗi user chỉ có 1 conversation AI
    Optional<ChatConversation> findByUserIdAndType(String userId, String type);

    // Thêm vào ChatConversationRepository
    Optional<ChatConversation> findFirstByUserIdAndTypeAndStatusOrderByUpdatedAtDesc(
            String userId, String type, String status
    );

    List<ChatConversation> findByUserIdAndTypeOrderByUpdatedAtDesc(String userId, String type);
    List<ChatConversation> findByTypeOrderByUpdatedAtDesc(String type);
}

