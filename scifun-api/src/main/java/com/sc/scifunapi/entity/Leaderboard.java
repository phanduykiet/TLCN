package com.sc.scifunapi.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.*;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Document(collection = "leaderboards")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@CompoundIndexes({
        // Compound index tương ứng Mongoose
        @CompoundIndex(name = "subject_period_rank", def = "{'subjectId': 1, 'period': 1, 'rank': 1}"),
        @CompoundIndex(name = "user_subject_period", def = "{'userId': 1, 'subjectId': 1, 'period': 1}"),
        @CompoundIndex(name = "period_score_progress", def = "{'period': 1, 'totalScore': -1, 'progressCreatedAt': 1}")
})
public class Leaderboard {

    @Id
    private String id;

    @Indexed
    private String subjectId;

    @Indexed
    private String userId;

    private String userName;

    private String userAvatar;

    private String subjectName;

    private double progress;

    private double averageScore;

    @Indexed
    private double totalScore;

    private int completedQuizzes;

    private int completedTopics;

    @Indexed
    private int rank;

    private Integer previousRank;

    private Date progressCreatedAt;

    private Date updatedAt;

    @Indexed
    private String period; // daily, weekly, monthly, alltime
}
