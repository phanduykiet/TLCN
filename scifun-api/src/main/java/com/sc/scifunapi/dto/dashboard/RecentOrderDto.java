package com.sc.scifunapi.dto.dashboard;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RecentOrderDto {

    private String id;
    private String plan;
    private Double total;
    private String status;
}