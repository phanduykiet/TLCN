package com.sc.scifunapi.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Document(collection = "questions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Question {

    @Id
    private String id;

    private String text;

    @DBRef
    private Quiz quiz;

    private List<Answer> answers;

    private String explanation;

    // Embedded Answer
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Answer {

        // giống _id của subdocument bên Mongoose
        @Builder.Default
        private String id = new org.bson.types.ObjectId().toHexString();

        private String text;
        private boolean isCorrect;
    }
}

