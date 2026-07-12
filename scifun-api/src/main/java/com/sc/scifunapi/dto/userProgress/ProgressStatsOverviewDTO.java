package com.sc.scifunapi.dto.userProgress;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProgressStatsOverviewDTO {
    private List<ProgressStatDTO> day;
    private List<ProgressStatDTO> week;
    private List<ProgressStatDTO> month;
}