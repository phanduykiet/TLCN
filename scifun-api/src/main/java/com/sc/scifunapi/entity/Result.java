package com.sc.scifunapi.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "results")
public class Result {

    @Id
    private String id;

    @Field("userId")
    private String userId;

    @DBRef(lazy = true)
    @Field("quiz")
    private Quiz quiz;

    @Field("bestScore")
    private Double bestScore;

    @Field("attempts")
    private Integer attempts;

    @Field("averageScore")
    private Double averageScore;

    @Field("lastSubmissionAt")
    private Date lastSubmissionAt;

    @Field("createdAt")
    private Date createdAt;
}
