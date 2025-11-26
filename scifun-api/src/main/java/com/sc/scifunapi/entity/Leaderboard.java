package com.sc.scifunapi.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "leaderboards")
@CompoundIndexes({
        @CompoundIndex(name = "subject_period_rank_idx", def = "{'subjectId': 1, 'period': 1, 'rank': 1}"),
        @CompoundIndex(name = "user_subject_period_idx", def = "{'userId': 1, 'subjectId': 1, 'period': 1}"),
        @CompoundIndex(name = "period_score_progress_idx", def = "{'period': 1, 'totalScore': -1, 'progressCreatedAt': 1}")
})
public class Leaderboard {

    @Id
    private String id;

    @Field("subjectId")
    @Indexed
    private String subjectId;

    @Field("userId")
    @Indexed
    private String userId;

    @Field("userName")
    private String userName;

    @Field("userAvatar")
    private String userAvatar;

    @Field("subjectName")
    private String subjectName;

    @Field("progress")
    private Double progress;

    @Field("averageScore")
    private Double averageScore;

    @Field("totalScore")
    @Indexed
    private Double totalScore;

    @Field("completedQuizzes")
    private Integer completedQuizzes;

    @Field("completedTopics")
    private Integer completedTopics;

    @Field("rank")
    @Indexed
    private Integer rank;

    @Field("previousRank")
    private Integer previousRank;

    @Field("progressCreatedAt")
    private Date progressCreatedAt;

    @Field("period")
    @Indexed
    private String period;     // "daily" | "weekly" | "monthly" | "alltime"

    @Field("createdAt")
    private Date createdAt;

    @Field("updatedAt")
    private Date updatedAt;
}
