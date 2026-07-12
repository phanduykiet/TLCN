package com.sc.scifunapi.service;

import com.sc.scifunapi.dto.submission.SubmissionStatProjection;
import com.sc.scifunapi.dto.userProgress.ProgressStatDTO;
import com.sc.scifunapi.dto.userProgress.ProgressStatsOverviewDTO;
import com.sc.scifunapi.entity.*;
import com.sc.scifunapi.enums.StatPeriod;
import com.sc.scifunapi.repository.*;
import lombok.RequiredArgsConstructor;
import java.util.function.Function;
import org.bson.Document;
import org.bson.types.ObjectId;
import com.mongodb.DBRef;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserProgressService {

    @Autowired
    private MongoTemplate mongoTemplate;
    private final SubjectRepository subjectRepository;
    private final TopicRepository topicRepository;
    private final QuizRepository quizRepository;
    private final ResultRepository resultRepository;
    private final UserProgressRepository userProgressRepository;
    private final SubmissionRepository submissionRepository;

    // Lấy tiến độ subject của user
    public UserProgress getUserProgressSv(String userId, String subjectId) {
        return initializeOrUpdateUserProgressSv(userId, subjectId);
    }

    // Khởi tạo hoặc cập nhật UserProgress
    public UserProgress initializeOrUpdateUserProgressSv(String userId, String subjectId) {
        // 1. Lấy thông tin subject
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new RuntimeException("Môn học không tồn tại"));

        // 2. Lấy tất cả topics thuộc subject
        List<Topic> topics = topicRepository.findBySubject_Id(subjectId);
        List<ObjectId> topicObjectIds = topics.stream()
                .map(t -> new ObjectId(t.getId()))
                .toList();

        // Lấy TẤT CẢ quiz thuộc các topic này trong 1 query duy nhất (raw, không qua entity mapping)
        Query quizQuery = new Query(Criteria.where("topic.$id").in(topicObjectIds));
        List<Document> allQuizDocs = mongoTemplate.find(quizQuery, Document.class, "quizzes");

        // Gom nhóm quiz theo topicId ngay trong Java, không cần resolve DBRef
        Map<String, List<Document>> quizzesByTopicId = new HashMap<>();
        for (Document doc : allQuizDocs) {
            Object topicRef = doc.get("topic");
            if (topicRef instanceof DBRef dbRef) {
                String topicId = dbRef.getId().toString();
                quizzesByTopicId.computeIfAbsent(topicId, k -> new ArrayList<>()).add(doc);
            }
        }

        // Lấy TẤT CẢ result của user trong 1 query duy nhất (raw, tránh resolve DBRef quiz)
        Query resultQuery = new Query(Criteria.where("userId").is(userId));
        List<Document> resultDocs = mongoTemplate.find(resultQuery, Document.class, "results");

        Map<String, Document> resultMap = new HashMap<>();
        for (Document doc : resultDocs) {
            Object quizRef = doc.get("quiz");
            if (quizRef instanceof DBRef dbRef) {
                resultMap.put(dbRef.getId().toString(), doc);
            }
        }

        List<UserProgress.TopicProgress> topicsData = new ArrayList<>();
        int totalQuizzes = 0;
        int totalCompletedQuizzes = 0;

        for (Topic topic : topics) {

            List<Document> quizDocs = quizzesByTopicId.getOrDefault(topic.getId(), List.of());
            totalQuizzes += quizDocs.size();

            List<UserProgress.QuizProgress> quizzesData = new ArrayList<>();
            int topicCompletedQuizzes = 0;
            double topicScoreSum = 0.0;

            for (Document quizDoc : quizDocs) {
                String quizId = quizDoc.getObjectId("_id").toHexString();
                Document result = resultMap.get(quizId);

                UserProgress.QuizProgress qp = new UserProgress.QuizProgress();
                qp.setQuizId(quizId);
                qp.setName(quizDoc.getString("title"));

                if (result != null) {
                    topicCompletedQuizzes++;
                    totalCompletedQuizzes++;

                    double avgScore = result.get("averageScore") != null
                            ? ((Number) result.get("averageScore")).doubleValue() : 0.0;
                    topicScoreSum += avgScore;

                    qp.setScore(avgScore);
                    qp.setBestScore(result.get("bestScore") != null
                            ? ((Number) result.get("bestScore")).doubleValue() : 0.0);
                    qp.setAttempts(result.get("attempts") != null
                            ? ((Number) result.get("attempts")).intValue() : 0);
                    qp.setLastSubmissionAt((Date) result.get("lastSubmissionAt"));
                } else {
                    qp.setScore(null);
                    qp.setBestScore(0.0);
                    qp.setAttempts(0);
                    qp.setLastSubmissionAt(null);
                }

                quizzesData.add(qp);
            }

            double topicProgress = quizDocs.size() > 0
                    ? (topicCompletedQuizzes * 100.0 / quizDocs.size())
                    : 0.0;

            double topicAvgScore = topicCompletedQuizzes > 0
                    ? (topicScoreSum / topicCompletedQuizzes)
                    : 0.0;

            UserProgress.TopicProgress tp = new UserProgress.TopicProgress();
            tp.setTopicId(topic.getId());
            tp.setName(topic.getName());
            tp.setProgress(round2(topicProgress));
            tp.setTotalQuizzes(quizDocs.size());
            tp.setCompletedQuizzes(topicCompletedQuizzes);
            tp.setAverageScore(round2(topicAvgScore));
            tp.setQuizzes(quizzesData);

            topicsData.add(tp);
        }

        // 3. Tính subject progress
        double percentPerTopic = topics.size() > 0 ? 100.0 / topics.size() : 0.0;
        double subjectProgress = 0.0;
        double totalScoreSum = 0.0;
        int completedTopics = 0;

        for (UserProgress.TopicProgress tp : topicsData) {
            subjectProgress += (tp.getProgress() / 100.0) * percentPerTopic;
            totalScoreSum += tp.getAverageScore() * tp.getCompletedQuizzes();

            if (tp.getProgress() == 100.0) {
                completedTopics++;
            }
        }

        double subjectAvgScore = totalCompletedQuizzes > 0
                ? (totalScoreSum / totalCompletedQuizzes)
                : 0.0;

        // 4. Upsert UserProgress
        Optional<UserProgress> optional = userProgressRepository.findByUserIdAndSubjectId(userId, subjectId);
        UserProgress userProgress = optional.orElseGet(UserProgress::new);

        userProgress.setUserId(userId);
        userProgress.setSubjectId(subjectId);
        userProgress.setSubjectName(subject.getName());

        userProgress.setProgress(round2(subjectProgress));
        userProgress.setTotalTopics(topics.size());
        userProgress.setCompletedTopics(completedTopics);
        userProgress.setTotalQuizzes(totalQuizzes);
        userProgress.setCompletedQuizzes(totalCompletedQuizzes);
        userProgress.setAverageScore(round2(subjectAvgScore));
        userProgress.setTopics(topicsData);
        userProgress.setLastUpdatedAt(new Date());

        return userProgressRepository.save(userProgress);
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    public ProgressStatsOverviewDTO getProgressStatsSv(String userId) {

        // Query 1 lần duy nhất, dùng chung cho cả 3 period
        List<SubmissionStatProjection> allSubmissions = submissionRepository
                .findStatProjectionsByUserId(userId);

        return new ProgressStatsOverviewDTO(
                computeStatsForPeriod(allSubmissions, StatPeriod.DAY),
                computeStatsForPeriod(allSubmissions, StatPeriod.WEEK),
                computeStatsForPeriod(allSubmissions, StatPeriod.MONTH)
        );
    }

    private List<ProgressStatDTO> computeStatsForPeriod(
            List<SubmissionStatProjection> allSubmissions, StatPeriod period) {

        LocalDate today = LocalDate.now();
        LocalDate from;
        LocalDate to;

        switch (period) {
            case WEEK -> {
                from = today.with(DayOfWeek.MONDAY);
                to = today.with(DayOfWeek.SUNDAY);
            }
            case MONTH -> {
                from = today.withDayOfMonth(1);
                to = today.withDayOfMonth(today.lengthOfMonth());
            }
            default -> {
                from = today;
                to = today;
            }
        }

        Date start = Date.from(from.atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date end = Date.from(to.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant());

        Map<String, SubmissionStatProjection> firstCompletionByQuiz = allSubmissions.stream()
                .collect(Collectors.toMap(
                        SubmissionStatProjection::getQuizId,
                        s -> s,
                        (s1, s2) -> s1.getCreatedAt().before(s2.getCreatedAt()) ? s1 : s2
                ));

        List<SubmissionStatProjection> firstCompletionsInRange = firstCompletionByQuiz.values().stream()
                .filter(s -> !s.getCreatedAt().before(start) && s.getCreatedAt().before(end))
                .toList();

        List<SubmissionStatProjection> submissionsInRange = allSubmissions.stream()
                .filter(s -> !s.getCreatedAt().before(start) && s.getCreatedAt().before(end))
                .toList();

        Map<String, List<SubmissionStatProjection>> activityGrouped = submissionsInRange.stream()
                .collect(Collectors.groupingBy(s -> formatKey(s.getCreatedAt(), period)));

        Map<String, List<SubmissionStatProjection>> newCompletionGrouped = firstCompletionsInRange.stream()
                .collect(Collectors.groupingBy(s -> formatKey(s.getCreatedAt(), period)));

        Set<String> allKeys = new TreeSet<>();
        allKeys.addAll(activityGrouped.keySet());
        allKeys.addAll(newCompletionGrouped.keySet());

        List<ProgressStatDTO> result = new ArrayList<>();
        for (String key : allKeys) {
            List<SubmissionStatProjection> activityList = activityGrouped.getOrDefault(key, List.of());
            List<SubmissionStatProjection> newCompleteList = newCompletionGrouped.getOrDefault(key, List.of());

            double avgScore = activityList.stream()
                    .mapToDouble(s -> s.getScore() != null ? s.getScore() : 0.0)
                    .average()
                    .orElse(0.0);

            result.add(new ProgressStatDTO(
                    key,
                    activityList.size(),
                    newCompleteList.size(),
                    Math.round(avgScore * 100.0) / 100.0
            ));
        }

        result.sort(Comparator.comparing(ProgressStatDTO::getPeriodLabel));
        return result;
    }

    private String formatKey(Date date, StatPeriod period) {
        LocalDateTime dt = date.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();

        return switch (period) {
            case WEEK -> {
                WeekFields wf = WeekFields.ISO;
                int week = dt.get(wf.weekOfWeekBasedYear());
                int year = dt.get(wf.weekBasedYear());
                yield year + "-W" + String.format("%02d", week);
            }
            case MONTH -> dt.getYear() + "-" + String.format("%02d", dt.getMonthValue());
            default -> dt.toLocalDate().toString();
        };
    }
}