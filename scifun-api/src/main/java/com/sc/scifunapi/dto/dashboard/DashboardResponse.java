package com.sc.scifunapi.dto.dashboard;

import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardResponse {

    private Long totalUsers;

    private Long totalQuizzes;

    private Long totalAttempts;

    private Double totalRevenue;

    private Long totalOrders;

    private List<MonthlyRevenueDto> monthlyRevenue;

    private List<PlanStatisticDto> planDistribution;

    private List<TopQuizDto> topQuizzes;

    private List<RecentUserDto> recentUsers;

    private List<RecentOrderDto> recentOrders;

    private List<ChartItemDto> subjectDistribution;

    private List<ChartItemDto> referralDistribution;

    private List<ChartItemDto> ageGroupDistribution;

    private List<ChartItemDto> levelDistribution;
}