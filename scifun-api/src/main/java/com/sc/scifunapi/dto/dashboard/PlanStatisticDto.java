package com.sc.scifunapi.dto.dashboard;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PlanStatisticDto {

    private String plan;
    private Long count;
}