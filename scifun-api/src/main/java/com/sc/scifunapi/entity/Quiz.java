package com.sc.scifunapi.entity;

import lombok.*;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "quizzes")
public class Quiz {

    @Id
    private String id;

    private String title;
    private String description;

    @DBRef(lazy = true)
    private Topic topic;

    @Builder.Default
    private Long uniqueUserCount = 0L;

    @Builder.Default
    private Date lastAttemptAt = new Date(0);

    @Builder.Default
    private Long favoriteCount = 0L;

    private Integer duration;

    @Builder.Default
    private Integer questionCount = 0;

    public enum AccessTier {
        FREE, PRO
    }

    private AccessTier accessTier;

    @CreatedDate
    private Date createdAt;

    @LastModifiedDate
    private Date updatedAt;
}
