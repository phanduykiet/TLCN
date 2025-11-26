package com.sc.scifunapi.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.List;

@Document(collection = "userprogresses")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProgress {

    @Id
    private String id;

    private String userId;
    private String subjectId;
    private String subjectName;

    private double progress;          // % hoàn thành subject (0-100)
    private int totalTopics;
    private int completedTopics;
    private int totalQuizzes;
    private int completedQuizzes;
    private double averageScore;

    private List<TopicProgress> topics;

    private Date lastUpdatedAt;
    private Date createdAt;
    private Date updatedAt;

    // ====== TopicProgress (inner document) ======
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopicProgress {
        private String topicId;
        private String name;
        private double progress;          // % hoàn thành topic
        private int totalQuizzes;
        private int completedQuizzes;
        private double averageScore;
        private List<QuizProgress> quizzes;
    }

    // ====== QuizProgress (inner document) ======
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuizProgress {
        private String quizId;
        private String name;
        private Double score;             // null = chưa làm
        private double bestScore;
        private int attempts;
        private Date lastSubmissionAt;
    }
}
