package com.sc.scifunapi.dto.userProgress;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ProgressStatDTO {
    private String periodLabel;     
    private long totalSubmissions;  
    private long completedQuizzes;  
    private double averageScore;    
}
