package com.sc.scifunapi.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import com.sc.scifunapi.dto.dashboard.ChartItemDto;
import com.sc.scifunapi.dto.dashboard.DashboardResponse;
import com.sc.scifunapi.dto.dashboard.MonthlyRevenueDto;
import com.sc.scifunapi.dto.dashboard.PlanStatisticDto;
import com.sc.scifunapi.dto.dashboard.RecentOrderDto;
import com.sc.scifunapi.dto.dashboard.RecentUserDto;
import com.sc.scifunapi.dto.dashboard.TopQuizDto;
import com.sc.scifunapi.entity.Order;
import com.sc.scifunapi.enums.OrderStatus;
import com.sc.scifunapi.repository.OrderRepository;
import com.sc.scifunapi.repository.QuizRepository;
import com.sc.scifunapi.repository.SubmissionRepository;
import com.sc.scifunapi.repository.UserRepository;
import com.sc.scifunapi.service.DashboardService;
import org.bson.Document;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import java.util.stream.Collectors;
import java.util.Calendar;
import java.util.Map;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl
                implements DashboardService {

        private final UserRepository userRepository;
        private final QuizRepository quizRepository;
        private final SubmissionRepository submissionRepository;
        private final OrderRepository orderRepository;
        private final MongoTemplate mongoTemplate;

        @Override
        public DashboardResponse getDashboard() {

                long totalUsers = userRepository.count();

                long totalQuizzes = quizRepository.count();

                long totalAttempts = submissionRepository.count();

                long totalOrders = orderRepository.count();

                double totalRevenue = orderRepository.findAll()
                                .stream()
                                .filter(o -> o.getStatus() == OrderStatus.PAID)
                                .mapToDouble(Order::getTotal)
                                .sum();

                return DashboardResponse.builder()
                                .totalUsers(totalUsers)
                                .totalQuizzes(totalQuizzes)
                                .totalAttempts(totalAttempts)
                                .totalOrders(totalOrders)
                                .totalRevenue(totalRevenue)

                                .topQuizzes(getTopQuizzes())

                                .monthlyRevenue(getMonthlyRevenue())

                                .planDistribution(getPlanDistribution())

                                .subjectDistribution(getSubjectDistribution())

                                .referralDistribution(getReferralDistribution())

                                .ageGroupDistribution(getAgeGroupDistribution())

                                .levelDistribution(getLevelDistribution())

                                .recentUsers(getRecentUsers())

                                .recentOrders(getRecentOrders())

                                .build();
        }

        private List<RecentUserDto> getRecentUsers() {

                return userRepository
                                .findTop5ByOrderByCreatedAtDesc()
                                .stream()
                                .map(user -> new RecentUserDto(
                                                user.getId(),
                                                user.getFullname(),
                                                user.getRole().name()))
                                .toList();
        }

        private List<RecentOrderDto> getRecentOrders() {

                return orderRepository
                                .findTop5ByOrderByCreatedAtDesc()
                                .stream()
                                .map(order -> new RecentOrderDto(
                                                order.getId(),
                                                order.getPlanTier().name(),
                                                order.getTotal(),
                                                order.getStatus().name()))
                                .toList();
        }

        private List<TopQuizDto> getTopQuizzes() {

                return quizRepository
                                .findTop5ByOrderByUniqueUserCountDesc()
                                .stream()
                                .map(q -> TopQuizDto.builder()
                                                .quizId(q.getId())
                                                .title(q.getTitle())
                                                .attempts(q.getUniqueUserCount())
                                                .build())
                                .toList();
        }

        private List<ChartItemDto> getSubjectDistribution() {

                Aggregation aggregation = Aggregation.newAggregation(
                                Aggregation.group("subject")
                                                .count()
                                                .as("value"));

                AggregationResults<Document> results = mongoTemplate.aggregate(
                                aggregation,
                                "user_onboardings",
                                Document.class);

                return results.getMappedResults()
                                .stream()
                                .map(doc -> new ChartItemDto(
                                                doc.getString("_id"),
                                                ((Number) doc.get("value")).longValue()))
                                .toList();
        }

        private List<ChartItemDto> getReferralDistribution() {

                Aggregation aggregation = Aggregation.newAggregation(
                                Aggregation.group("referralSource")
                                                .count()
                                                .as("value"));

                AggregationResults<Document> results = mongoTemplate.aggregate(
                                aggregation,
                                "user_onboardings",
                                Document.class);

                return results.getMappedResults()
                                .stream()
                                .map(doc -> new ChartItemDto(
                                                doc.getString("_id"),
                                                ((Number) doc.get("value")).longValue()))
                                .toList();
        }

        private List<PlanStatisticDto> getPlanDistribution() {

                return orderRepository.findAll()
                                .stream()
                                .filter(o -> o.getStatus() == OrderStatus.PAID)
                                .collect(Collectors.groupingBy(
                                                o -> o.getPlanTier().name(),
                                                Collectors.counting()))
                                .entrySet()
                                .stream()
                                .map(entry -> new PlanStatisticDto(
                                                entry.getKey(),
                                                entry.getValue()))
                                .toList();
        }

        private List<MonthlyRevenueDto> getMonthlyRevenue() {

                Map<Integer, Double> revenueByMonth = orderRepository.findAll()
                                .stream()
                                .filter(o -> o.getStatus() == OrderStatus.PAID)
                                .collect(Collectors.groupingBy(
                                                order -> {
                                                        Calendar calendar = Calendar.getInstance();
                                                        calendar.setTime(order.getCreatedAt());
                                                        return calendar.get(Calendar.MONTH) + 1;
                                                },
                                                Collectors.summingDouble(Order::getTotal)));

                return revenueByMonth.entrySet()
                                .stream()
                                .sorted(Map.Entry.comparingByKey())
                                .map(entry -> new MonthlyRevenueDto(
                                                "T" + entry.getKey(),
                                                entry.getValue()))
                                .toList();
        }

        private List<ChartItemDto> getAgeGroupDistribution() {

                Aggregation aggregation = Aggregation.newAggregation(
                                Aggregation.group("ageGroup")
                                                .count()
                                                .as("value"));

                AggregationResults<Document> results = mongoTemplate.aggregate(
                                aggregation,
                                "user_onboardings",
                                Document.class);

                return results.getMappedResults()
                                .stream()
                                .map(doc -> new ChartItemDto(
                                                doc.getString("_id"),
                                                ((Number) doc.get("value")).longValue()))
                                .toList();
        }

        private List<ChartItemDto> getLevelDistribution() {

                Aggregation aggregation = Aggregation.newAggregation(
                                Aggregation.group("level")
                                                .count()
                                                .as("value"));

                AggregationResults<Document> results = mongoTemplate.aggregate(
                                aggregation,
                                "user_onboardings",
                                Document.class);

                return results.getMappedResults()
                                .stream()
                                .map(doc -> new ChartItemDto(
                                                doc.getString("_id"),
                                                ((Number) doc.get("value")).longValue()))
                                .toList();
        }

}
