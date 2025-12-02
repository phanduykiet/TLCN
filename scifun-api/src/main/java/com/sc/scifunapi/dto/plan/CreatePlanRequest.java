package com.sc.scifunapi.dto.plan;

import lombok.Data;

@Data
public class CreatePlanRequest {
    private String name;
    private Double price;
    private Integer durationDays;
}
