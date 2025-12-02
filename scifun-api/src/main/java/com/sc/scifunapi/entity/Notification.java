// src/main/java/com/sc/scifunapi/entity/Notification.java
package com.sc.scifunapi.entity;

import com.sc.scifunapi.enums.NotificationType;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "notifications")
public class Notification {

    @Id
    private String id;

    @Indexed
    private String userId;

    private NotificationType type;

    private String title;

    private String message;

    // Dữ liệu phụ – Mixed
    private Map<String, Object> data;

    private String link;

    @Builder.Default
    private boolean isRead = false;

    @Builder.Default
    private Date createdAt = new Date();
}
