package com.sc.scifunapi.service;

import com.sc.scifunapi.entity.*;
import com.sc.scifunapi.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class UserProgressService {

    private final SubjectRepository subjectRepository;
    private final TopicRepository topicRepository;
    private final QuizRepository quizRepository;
    private final ResultRepository resultRepository;
    private final UserProgressRepository userProgressRepository;

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
        // Cần method trong TopicRepository: List<Topic> findBySubject_Id(String subjectId);
        List<Topic> topics = topicRepository.findBySubject_Id(subjectId);

        List<UserProgress.TopicProgress> topicsData = new ArrayList<>();
        int totalQuizzes = 0;
        int totalCompletedQuizzes = 0;

        for (Topic topic : topics) {

            // Lấy quizzes thuộc topic
            // Cần method trong QuizRepository: List<Quiz> findByTopic_Id(String topicId);
            List<Quiz> quizzes = quizRepository.findByTopic_Id(topic.getId());
            totalQuizzes += quizzes.size();

            List<UserProgress.QuizProgress> quizzesData = new ArrayList<>();
            int topicCompletedQuizzes = 0;
            double topicScoreSum = 0.0;

            for (Quiz quiz : quizzes) {
                // Kiểm tra xem user đã làm quiz này chưa
                Result result = resultRepository.findByUserIdAndQuiz_Id(userId, quiz.getId());

                UserProgress.QuizProgress qp = new UserProgress.QuizProgress();
                qp.setQuizId(quiz.getId());
                qp.setName(quiz.getTitle());

                if (result != null) {
                    topicCompletedQuizzes++;
                    totalCompletedQuizzes++;
                    topicScoreSum += result.getAverageScore();

                    qp.setScore(result.getAverageScore());
                    qp.setBestScore(result.getBestScore());
                    qp.setAttempts(result.getAttempts());
                    qp.setLastSubmissionAt(result.getLastSubmissionAt());
                } else {
                    qp.setScore(null);
                    qp.setBestScore(0.0);
                    qp.setAttempts(0);
                    qp.setLastSubmissionAt(null);
                }

                quizzesData.add(qp);
            }

            // Tính progress và average score của topic
            double topicProgress = quizzes.size() > 0
                    ? (topicCompletedQuizzes * 100.0 / quizzes.size())
                    : 0.0;

            double topicAvgScore = topicCompletedQuizzes > 0
                    ? (topicScoreSum / topicCompletedQuizzes)
                    : 0.0;

            UserProgress.TopicProgress tp = new UserProgress.TopicProgress();
            tp.setTopicId(topic.getId());
            tp.setName(topic.getName());
            tp.setProgress(round2(topicProgress));
            tp.setTotalQuizzes(quizzes.size());
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
}