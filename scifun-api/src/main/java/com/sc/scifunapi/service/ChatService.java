package com.sc.scifunapi.service;

import com.sc.scifunapi.dto.chat.SendChatMessageRequest;
import com.sc.scifunapi.entity.ChatConversation;
import com.sc.scifunapi.entity.ChatMessage;
import com.sc.scifunapi.entity.User;
import com.sc.scifunapi.repository.ChatConversationRepository;
import com.sc.scifunapi.repository.ChatMessageRepository;
import com.sc.scifunapi.repository.UserRepository;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class ChatService {

    private final ChatConversationRepository conversationRepo;
    private final ChatMessageRepository messageRepo;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserRepository userRepository;

    public ChatService(ChatConversationRepository conversationRepo,
            ChatMessageRepository messageRepo,
            SimpMessagingTemplate messagingTemplate,
            UserRepository userRepository) {
        this.conversationRepo = conversationRepo;
        this.messageRepo = messageRepo;
        this.messagingTemplate = messagingTemplate;
        this.userRepository = userRepository;
    }

    /** User mở chat: lấy phòng OPEN nếu có, không thì tạo mới */
    public ChatConversation getOrCreateConversationForUser(String userId) {
        return conversationRepo.findFirstByUserIdAndStatusOrderByUpdatedAtDesc(userId, "OPEN")
                .orElseGet(() -> conversationRepo.save(ChatConversation.builder()
                        .userId(userId)
                        .adminId(null) // chưa assign
                        .status("OPEN")
                        .createdAt(new Date())
                        .updatedAt(new Date())
                        .build()));
    }

    /** Gửi tin nhắn + lưu DB + realtime */
    public ChatMessage sendMessage(String conversationId, String senderId, String senderRole, String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Nội dung không được trống");
        }

        Date now = new Date();

        // ✅ Nếu conversationId chưa tồn tại thì tạo mới luôn
        ChatConversation convo = conversationRepo.findById(conversationId).orElse(null);

        if (convo == null) {
            convo = ChatConversation.builder()
                    .id(conversationId) // cho phép set id theo chuỗi test-room-1
                    .userId(senderRole.equals("ADMIN") ? null : senderId)
                    .adminId(senderRole.equals("ADMIN") ? senderId : null)
                    .status("OPEN")
                    .createdAt(now)
                    .updatedAt(now)
                    .build();
        } else {
            convo.setUpdatedAt(now);
        }

        conversationRepo.save(convo);

        String senderName = userRepository.findById(senderId)
                .map(User::getFullname) // thay đúng getter của bạn
                .orElse("Unknown");

        ChatMessage msg = ChatMessage.builder()
                .conversationId(conversationId)
                .senderId(senderId)
                .senderRole(senderRole)
                .content(content.trim())
                .createdAt(now)
                .build();

        ChatMessage saved = messageRepo.save(msg);

        Map<String, Object> payload = new HashMap<>();
        payload.put("id", saved.getId());
        payload.put("conversationId", saved.getConversationId());
        payload.put("senderId", saved.getSenderId());
        payload.put("senderName", senderName); // ← thêm
        payload.put("senderRole", saved.getSenderRole());
        payload.put("content", saved.getContent());
        payload.put("createdAt", saved.getCreatedAt());

        System.out.println("[WS CHAT] new message -> " + payload);

        // ✅ Broadcast
        String dest = "/topic/chat/" + conversationId;
        messagingTemplate.convertAndSend(dest, payload);

        System.out.println("[WS CHAT] broadcast -> " + dest);

        return saved;
    }

    /** Assign admin (tuỳ chọn) */
    public ChatConversation assignAdmin(String conversationId, String adminId) {
        ChatConversation convo = conversationRepo.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy conversation"));
        convo.setAdminId(adminId);
        convo.setUpdatedAt(new Date());
        return conversationRepo.save(convo);
    }
}
