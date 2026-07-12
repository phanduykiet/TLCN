package com.sc.scifunapi.dto.submission;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubmissionStatProjection {
    private String id;
    private String quizId;
    private Double score;
    private Date createdAt;
}