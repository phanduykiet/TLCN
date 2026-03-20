package com.sc.scifunapi.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "chat_conversations")
public class ChatConversation {

    @Id
    private String id;

    @Indexed
    private String userId;

    @Indexed
    private String adminId;       // null nếu là AI conversation

    @Builder.Default
    private String type = "HUMAN"; // "HUMAN" | "AI"

    @Builder.Default
    private String status = "OPEN";

    @Builder.Default
    private Date createdAt = new Date();

    @Builder.Default
    private Date updatedAt = new Date();
}