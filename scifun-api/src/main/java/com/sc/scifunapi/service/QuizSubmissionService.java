package com.sc.scifunapi.service;

import com.sc.scifunapi.entity.Question;
import com.sc.scifunapi.entity.Quiz;
import com.sc.scifunapi.entity.Result;
import com.sc.scifunapi.entity.Submission;
import com.sc.scifunapi.repository.QuestionRepository;
import com.sc.scifunapi.repository.QuizRepository;
import com.sc.scifunapi.repository.ResultRepository;
import com.sc.scifunapi.repository.SubmissionRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuizSubmissionService {

    private final QuestionRepository questionRepository;
    private final SubmissionRepository submissionRepository;
    private final ResultRepository resultRepository;
    private final QuizRepository quizRepository;

    /**
     * Nộp bài + chấm điểm (giữ nguyên logic như Express)
     * payload:
     * {
     *   quizId: string,
     *   userId?: string,
     *   answers: [
     *     { questionId: string, selectedAnswerId: string }
     *   ]
     * }
     */
    @Transactional
    public Map<String, Object> handleSubmitQuizSv(Map<String, Object> payload) {

        String quizId = payload.get("quizId") != null ? payload.get("quizId").toString() : null;
        String userId = payload.get("userId") != null ? payload.get("userId").toString() : null;

        if (quizId == null || quizId.isBlank()) {
            throw new RuntimeException("Thiếu quizId");
        }

        // ===== Parse answers từ payload =====
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> answersRaw =
                (List<Map<String, Object>>) payload.get("answers");

        if (answersRaw == null || answersRaw.isEmpty()) {
            throw new RuntimeException("Thiếu danh sách answers");
        }

        // Lấy danh sách questionId (unique)
        Set<String> questionIds = answersRaw.stream()
                .map(a -> a.get("questionId").toString())
                .collect(Collectors.toSet());

        // Lấy question từ DB
        List<Question> questions = questionRepository.findAllById(questionIds);

        Map<String, Question> qMap = new HashMap<>();
        for (Question q : questions) {
            qMap.put(q.getId(), q);
        }

        int correctCount = 0;

        // ===== Build detailed answers để lưu vào Submission =====
        List<Submission.AnswerDetail> detailedAnswers = new ArrayList<>();

        for (Map<String, Object> ans : answersRaw) {

            String qId = ans.get("questionId").toString();
            String selectedAnswerId = ans.get("selectedAnswerId") != null
                    ? ans.get("selectedAnswerId").toString()
                    : null;

            Question q = qMap.get(qId);
            boolean isCorrect = false;

            if (q != null && selectedAnswerId != null) {
                if (q.getAnswers() != null) {
                    for (Question.Answer opt : q.getAnswers()) {
                        // so sánh theo id của Answer trong embedded list
                        if (opt.getId() != null
                                && opt.getId().equals(selectedAnswerId)
                                && opt.isCorrect()) {
                            isCorrect = true;
                            break;
                        }
                    }
                }
            }

            if (isCorrect) correctCount++;

            // Tạo Question ref chỉ với id (DBRef)
            Question qRef = new Question();
            qRef.setId(qId);

            Submission.AnswerDetail detail = Submission.AnswerDetail.builder()
                    .id(new ObjectId().toString())          // _id riêng cho từng answer
                    .question(qRef)
                    .selectedAnswer(selectedAnswerId)
                    .isCorrect(isCorrect)
                    .build();

            detailedAnswers.add(detail);
        }

        int totalQuestions = questionIds.size();
        double score = totalQuestions > 0 ? (correctCount * 100.0 / totalQuestions) : 0;

        // ===== Nếu userId null → không lưu DB, chỉ trả kết quả chấm =====
        if (userId == null || userId.isBlank()) {
            return Map.of(
                    "quizId", quizId,
                    "score", score,
                    "totalQuestions", totalQuestions,
                    "correctAnswers", correctCount
            );
        }

        // ===== Lưu Submission =====
        Quiz quizRef = new Quiz();
        quizRef.setId(quizId);

        Submission submission = Submission.builder()
                .userId(userId)
                .quiz(quizRef)
                .answers(detailedAnswers)
                .score(score)
                .build();

        submission = submissionRepository.save(submission);

        // ===== Cập nhật Result =====
        Result oldResult = resultRepository.findByUserIdAndQuiz_Id(userId, quizId);

        if (oldResult == null) {
            oldResult = Result.builder()
                    .userId(userId)
                    .quiz(quizRef)
                    .bestScore(score)
                    .attempts(1)
                    .averageScore(score)
                    .lastSubmissionAt(new Date())
                    .build();
        } else {
            int newAttempts = oldResult.getAttempts() + 1;
            double newAvg = ((oldResult.getAverageScore() * (newAttempts - 1)) + score) / newAttempts;

            oldResult.setAttempts(newAttempts);
            oldResult.setAverageScore(Math.round(newAvg * 100.0) / 100.0);
            oldResult.setBestScore(Math.max(oldResult.getBestScore(), score));
            oldResult.setLastSubmissionAt(new Date());
        }

        resultRepository.save(oldResult);

        // ===== Cập nhật thống kê quiz (uniqueUserCount, lastAttemptAt) =====
        updateQuizStatistics(quizId);

        // ===== Trả về kết quả giống Express =====
        return Map.of(
                "submissionId", submission.getId(),
                "quizId", quizId,
                "score", score,
                "totalQuestions", totalQuestions,
                "correctAnswers", correctCount
        );
    }

    /**
     * Cập nhật uniqueUserCount và lastAttemptAt cho Quiz
     */
    private void updateQuizStatistics(String quizId) {
        long uniqueUserCount = resultRepository.countByQuiz_Id(quizId);

        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz không tồn tại"));

        quiz.setUniqueUserCount(uniqueUserCount);
        quiz.setLastAttemptAt(new Date());

        quizRepository.save(quiz);
    }

    // Xem chi tiết bài làm + giải thích
    public Map<String, Object> getSubmissionDetailSv(String submissionId) {

        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy submission"));

        Quiz quiz = submission.getQuiz();

        // build quiz summary giống style Express (simple thôi)
        Map<String, Object> quizMap = buildQuizSummary(quiz);

        List<Map<String, Object>> answerDetails = new ArrayList<>();

        for (Submission.AnswerDetail a : submission.getAnswers()) {
            Question question = a.getQuestion();
            if (question == null) continue;

            List<Question.Answer> allAnswers = question.getAnswers() != null
                    ? question.getAnswers()
                    : List.of();

            // text đáp án user chọn
            String selectedAnswerText = allAnswers.stream()
                    .filter(opt -> opt.getId().equals(a.getSelectedAnswer()))
                    .map(Question.Answer::getText)
                    .findFirst()
                    .orElse(a.getSelectedAnswer());

            // tất cả đáp án đúng
            List<String> correctAnswers = allAnswers.stream()
                    .filter(Question.Answer::isCorrect)
                    .map(Question.Answer::getText)
                    .toList();

            Map<String, Object> m = new HashMap<>();
            m.put("questionId", question.getId());
            m.put("questionText", question.getText());
            m.put("selectedAnswer", selectedAnswerText);
            m.put("correctAnswers", correctAnswers);
            m.put("isCorrect", a.isCorrect());
            m.put("explanation", question.getExplanation());

            answerDetails.add(m);
        }

        Map<String, Object> res = new HashMap<>();
        res.put("submissionId", submission.getId());
        res.put("quiz", quizMap);
        res.put("score", submission.getScore());
        res.put("answers", answerDetails);

        return res;
    }

    // helper build quiz map gọn gàng
    private Map<String, Object> buildQuizSummary(Quiz quiz) {
        if (quiz == null) return null;

        Map<String, Object> m = new HashMap<>();
        m.put("_id", quiz.getId());
        m.put("title", quiz.getTitle());
        m.put("description", quiz.getDescription());
        m.put("duration", quiz.getDuration());
        m.put("questionCount", quiz.getQuestionCount());
        m.put("uniqueUserCount",
                quiz.getUniqueUserCount() != null ? quiz.getUniqueUserCount() : 0L);
        m.put("favoriteCount",
                quiz.getFavoriteCount() != null ? quiz.getFavoriteCount() : 0L);
        m.put("lastAttemptAt", quiz.getLastAttemptAt());
        m.put("accessTier", quiz.getAccessTier() != null ? quiz.getAccessTier().name() : "FREE");
        return m;
    }

    // LẤY DANH SÁCH RESULT (PHÂN TRANG)
    public Map<String, Object> getResultsSv(int page, int limit) {

        // PageRequest: sort theo lastSubmissionAt DESC giống .sort({ lastSubmissionAt: -1 })
        Pageable pageable = PageRequest.of(
                page - 1,
                limit,
                Sort.by(Sort.Direction.DESC, "lastSubmissionAt")
        );

        Page<Result> resultPage = resultRepository.findAll(pageable);

        // Map từng Result -> object "gọn" giống Express
        List<Map<String, Object>> data = resultPage.getContent().stream().map(r -> {
            Map<String, Object> map = new HashMap<>();
            map.put("_id", r.getId());
            map.put("userId", r.getUserId());

            // quiz (populate nhẹ giống Express)
            Quiz quiz = r.getQuiz();
            if (quiz != null) {
                Map<String, Object> quizMap = new HashMap<>();
                quizMap.put("_id", quiz.getId());
                quizMap.put("title", quiz.getTitle());
                quizMap.put("description", quiz.getDescription());
                quizMap.put("duration", quiz.getDuration());
                quizMap.put("questionCount", quiz.getQuestionCount());
                quizMap.put("accessTier",
                        quiz.getAccessTier() != null ? quiz.getAccessTier().name() : "FREE");
                map.put("quiz", quizMap);
            } else {
                map.put("quiz", null);
            }

            map.put("bestScore", r.getBestScore());
            map.put("attempts", r.getAttempts());
            map.put("averageScore", r.getAverageScore());
            map.put("lastSubmissionAt", r.getLastSubmissionAt());
            map.put("createdAt", r.getCreatedAt());

            return map;
        }).toList();

        long total = resultPage.getTotalElements();
        int totalPages = resultPage.getTotalPages();

        Map<String, Object> res = new HashMap<>();
        res.put("page", page);
        res.put("limit", limit);
        res.put("total", total);
        res.put("totalPages", totalPages);
        res.put("data", data);

        return res;
    }
}
