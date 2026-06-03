package com.sc.scifunapi.dto.chat;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ChatMessageDTO {
    private String id;
    private String conversationId;
    private String senderId;
    private String senderName; // ← thêm field này
    private String senderRole;
    private String content;
    private Date createdAt;
}
