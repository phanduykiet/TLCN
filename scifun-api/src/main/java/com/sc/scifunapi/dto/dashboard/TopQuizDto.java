package com.sc.scifunapi.dto.dashboard;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopQuizDto {

    private String quizId;
    private String title;
    private Long attempts;
}