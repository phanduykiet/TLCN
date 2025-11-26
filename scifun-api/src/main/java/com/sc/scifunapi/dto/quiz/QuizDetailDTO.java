package com.sc.scifunapi.dto.quiz;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuizDetailDTO {
    private String _id;
    private String title;
    private String description;
    private TopicSimpleDTO topic;
    private long uniqueUserCount;
    private Date lastAttemptAt;
    private long favoriteCount;
    private int duration;
    private int questionCount;
    private String accessTier;
    private Date createdAt;
    private Date updatedAt;
    private boolean isLocked;
}
