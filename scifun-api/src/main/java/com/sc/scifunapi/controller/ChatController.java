package com.sc.scifunapi.controller;

import com.sc.scifunapi.entity.*;
import com.sc.scifunapi.repository.*;
import com.sc.scifunapi.service.GroqService;
import com.sc.scifunapi.service.QuestionService;
import com.sc.scifunapi.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
public class ChatController {

    private final GroqService groqService;
    private final UserService userService;
    private final QuestionService questionService;
    private final UserOnboardingRepository userOnboardingRepository;
    private final ChatConversationRepository chatConversationRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final TopicRepository topicRepository;
    private final SubjectRepository subjectRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @PostMapping("/ask")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> ask(@RequestBody Map<String, String> body) {
        try {
            String authUserId = (String) SecurityContextHolder.getContext()
                    .getAuthentication().getDetails();
            String message = body.get("message");

            // Lấy hoặc tạo AI conversation cho user
            ChatConversation conversation = chatConversationRepository
                    .findByUserIdAndType(authUserId, "AI")
                    .orElseGet(() -> chatConversationRepository.save(
                            ChatConversation.builder()
                                    .userId(authUserId)
                                    .type("AI")
                                    .build()
                    ));

            // Load lịch sử
            List<Map<String, String>> history = chatMessageRepository
                    .findTop20ByConversationIdOrderByCreatedAtAsc(conversation.getId())
                    .stream()
                    .map(m -> Map.of(
                            "role", "ADMIN".equals(m.getSenderRole()) ? "assistant" : "user",
                            "content", m.getContent()
                    ))
                    .toList();

            // Lấy thông tin user (giữ nguyên như cũ)
            User user = userService.findById(authUserId);
            var onboarding = userOnboardingRepository.findByUserId(authUserId).orElse(null);
            String subject  = onboarding != null ? onboarding.getSubject()  : "chưa chọn";
            String level    = onboarding != null ? onboarding.getLevel()    : "chưa chọn";
            String ageGroup = onboarding != null ? onboarding.getAgeGroup() : "chưa rõ";
            List<Subject> subjects = subjectRepository.findAll();
            List<Topic> topics     = topicRepository.findAll();
            List<Map<String, Object>> relatedQuestions = questionService.findRelatedQuestions(message);

            // Gọi AI
            String reply = groqService.chat(
                    message, user.getFullname(), user.getAge(), ageGroup,
                    subject, level, subjects, topics,
                    relatedQuestions, history
            );

            // Lưu tin nhắn user
            chatMessageRepository.save(ChatMessage.builder()
                    .conversationId(conversation.getId())
                    .senderId(authUserId)
                    .senderRole("USER")
                    .content(message)
                    .build());

            // Lưu reply của AI — dùng "AI" làm senderId cho dễ phân biệt
            ChatMessage aiMessage = chatMessageRepository.save(ChatMessage.builder()
                    .conversationId(conversation.getId())
                    .senderId("AI")
                    .senderRole("ADMIN")   // tái dụng ADMIN vì Groq đóng vai trợ lý
                    .content(reply)
                    .build());

            // Cập nhật updatedAt conversation
            conversation.setUpdatedAt(new Date());
            chatConversationRepository.save(conversation);

            // Broadcast reply AI qua WS (dùng object đã save, có id đầy đủ)
            messagingTemplate.convertAndSend("/topic/chat/" + conversation.getId(), aiMessage);


            return ResponseEntity.ok(Map.of("status", 200,
                    "data", Map.of("reply", reply)));

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", 400, "message", e.getMessage()));
        }
    }
}
