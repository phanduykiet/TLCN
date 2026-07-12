package com.sc.scifunapi.entity;

import lombok.*;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Document(collection = "submissions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Submission {

    @Id
    private String id;

    private String userId;

    @DBRef
    private Quiz quiz;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnswerDetail {

        // 👇 trường _id cho từng answer (giống Mongoose)
        @Field("_id")
        private String id;

        @DBRef(lazy = true)
        private Question question;

        // NEW: nhiều đáp án user chọn
        private List<String> selectedAnswers;

        private boolean isCorrect;
    }

    // danh sách câu trả lời chi tiết
    @Builder.Default
    private List<AnswerDetail> answers = new ArrayList<>();

    private Double score;

    @Builder.Default
    private Date createdAt = new Date();
}
