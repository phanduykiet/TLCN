package com.sc.scifunapi.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Document(collection = "favoritequizzes")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@CompoundIndex(name = "unique_user_quiz", def = "{'user': 1, 'quiz': 1}", unique = true)
public class FavoriteQuiz {

    @Id
    private String id;

    @Indexed
    private String user;   // userId (string), không cần DBRef để tránh nặng query

    @Indexed
    private String quiz;   // quizId

    @Builder.Default
    private Date createdAt = new Date();
}
