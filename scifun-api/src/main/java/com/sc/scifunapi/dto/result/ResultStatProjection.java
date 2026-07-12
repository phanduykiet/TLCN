package com.sc.scifunapi.dto.result;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResultStatProjection {
    private String id;
    private String quizId;
    private Double bestScore;
    private Integer attempts;
    private Double averageScore;
    private Date lastSubmissionAt;
}