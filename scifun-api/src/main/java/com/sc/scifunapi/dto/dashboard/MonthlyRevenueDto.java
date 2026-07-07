package com.sc.scifunapi.dto.dashboard;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MonthlyRevenueDto {

    private String month;
    private Double value;
}