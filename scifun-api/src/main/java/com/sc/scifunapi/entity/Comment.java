// src/main/java/com/sc/scifunapi/entity/Comment.java
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
@Document(collection = "comments")
public class Comment {

    @Id
    private String id;

    @Indexed
    private String userId;       // ref User

    private String userName;

    private String userAvatar;

    private String content;

    @Indexed
    private String parentId;     // null = comment gốc

    @Builder.Default
    private int repliesCount = 0;

    @Builder.Default
    private Date createdAt = new Date();

    @Builder.Default
    private Date updatedAt = new Date();
}
