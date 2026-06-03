package com.sc.scifunapi.service;

import com.sc.scifunapi.dto.chat.ChatMessageDTO;
import com.sc.scifunapi.entity.ChatMessage;
import com.sc.scifunapi.entity.User;
import com.sc.scifunapi.repository.ChatMessageRepository;
import com.sc.scifunapi.repository.UserRepository;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class ChatMessageService {

    private final ChatMessageRepository messageRepo;
    private final UserRepository userRepository;

    public ChatMessageService(ChatMessageRepository messageRepo, UserRepository userRepository) {
        this.messageRepo = messageRepo;
        this.userRepository = userRepository;
    }

    public Page<ChatMessage> getMessages(String conversationId, int page, int limit) {
        return messageRepo.findByConversationId(
                conversationId,
                PageRequest.of(page - 1, limit, Sort.by("createdAt").ascending()));
    }

    public Page<ChatMessageDTO> getMessagesTest(String conversationId, int page, int limit) {
        Page<ChatMessage> messages = messageRepo.findByConversationId(
                conversationId,
                PageRequest.of(page - 1, limit, Sort.by("createdAt").ascending()));

        // Batch load tất cả users trong 1 query thay vì N queries
        Set<String> senderIds = messages.stream()
                .map(ChatMessage::getSenderId)
                .collect(Collectors.toSet());

        Map<String, String> senderNameMap = userRepository.findAllById(senderIds).stream()
                .collect(Collectors.toMap(User::getId, User::getFullname));

        return messages.map(msg -> new ChatMessageDTO(
                msg.getId(),
                msg.getConversationId(),
                msg.getSenderId(),
                senderNameMap.getOrDefault(msg.getSenderId(), "Unknown"),
                msg.getSenderRole(),
                msg.getContent(),
                msg.getCreatedAt()));
    }
}
