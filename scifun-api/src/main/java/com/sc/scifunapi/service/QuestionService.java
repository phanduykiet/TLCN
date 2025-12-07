package com.sc.scifunapi.service;

import com.sc.scifunapi.entity.*;
import com.sc.scifunapi.enums.SubscriptionStatus;
import com.sc.scifunapi.enums.SubscriptionTier;
import com.sc.scifunapi.repository.QuestionRepository;
import com.sc.scifunapi.repository.QuizRepository;
import com.sc.scifunapi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

@Service
@RequiredArgsConstructor
public class QuestionService {

    private final QuestionRepository questionRepository;
    private final QuizRepository quizRepository;
    private final UserRepository userRepository;

    // Thêm câu hỏi
    // Thêm câu hỏi
    public Map<String, Object> createQuestionSv(Map<String, Object> data) {

        String text = data.get("text") != null ? data.get("text").toString() : null;
        String quizId = data.get("quiz") != null ? data.get("quiz").toString() : null;

        if (text == null || text.isBlank()) {
            throw new RuntimeException("Nội dung câu hỏi không được để trống");
        }
        if (quizId == null || quizId.isBlank()) {
            throw new RuntimeException("Quiz ID không được để trống");
        }

        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz không tồn tại"));

        List<Map<String, Object>> answersRaw =
                (List<Map<String, Object>>) data.get("answers");

        if (answersRaw == null || answersRaw.isEmpty()) {
            throw new RuntimeException("Danh sách đáp án không được để trống");
        }

        List<Question.Answer> answers = answersRaw.stream()
                .map(a -> Question.Answer.builder()
                        .text(a.get("text").toString())
                        .isCorrect(Boolean.parseBoolean(a.get("isCorrect").toString()))
                        .build()
                )
                .toList();


        Question question = Question.builder()
                .text(text)
                .quiz(quiz)
                .answers(answers)
                .explanation((String) data.get("explanation"))
                .build();

        Question saved = questionRepository.save(question);

        // ===== Trả về giống Express =====
        Map<String, Object> res = new HashMap<>();
        res.put("_id", saved.getId());
        res.put("text", saved.getText());
        res.put("explanation", saved.getExplanation());
        res.put("answers", saved.getAnswers());

        Map<String, Object> quizMap = new HashMap<>();
        quizMap.put("_id", quiz.getId());
        quizMap.put("title", quiz.getTitle());
        quizMap.put("description", quiz.getDescription());
        res.put("quiz", quizMap);

        return res;
    }

    private void checkQuizAccess(String userId, String quizId) {
        if (quizId == null || quizId.isBlank()) {
            throw new RuntimeException("Thiếu quizId");
        }

        // 1) Lấy thông tin quiz
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy quiz"));

        // 2) FREE → ai cũng truy cập được
        if (quiz.getAccessTier() == Quiz.AccessTier.FREE) {
            return;
        }

        // 3) PRO → cần userId
        if (userId == null) {
            throw new RuntimeException("Cần đăng nhập để làm quiz PRO");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        Subscription sub = user.getSubscription();

        if (sub == null) {
            throw new RuntimeException("Tài khoản của bạn chưa có gói PRO hoặc đã hết hạn");
        }

        boolean isPro =
                sub.getStatus() == SubscriptionStatus.ACTIVE &&
                        sub.getTier() == SubscriptionTier.PRO &&
                        sub.getCurrentPeriodEnd() != null &&
                        sub.getCurrentPeriodEnd().getTime() > System.currentTimeMillis();

        if (!isPro) {
            throw new RuntimeException("Tài khoản của bạn chưa có gói PRO hoặc đã hết hạn");
        }
    }

    // lấy danh sách câu hỏi
    public Map<String, Object> getQuestionsSv(int page, int limit, String quizId, String userId) {

        // 1. Check quyền truy cập quiz (FREE / PRO)
        checkQuizAccess(userId, quizId);

        // 2. Build pageable
        Pageable pageable = PageRequest.of(page - 1, limit, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Question> questionPage;

        if (quizId != null && !quizId.isBlank()) {
            questionPage = questionRepository.findByQuiz_Id(quizId, pageable);
        } else {
            questionPage = questionRepository.findAll(pageable);
        }

        // 3. Map Question -> JSON giống Express
        List<Map<String, Object>> questionList = questionPage.getContent().stream().map(q -> {
            Map<String, Object> qMap = new HashMap<>();
            qMap.put("_id", q.getId());
            qMap.put("text", q.getText());
            qMap.put("explanation", q.getExplanation());

            // ---- quiz ----
            Quiz quiz = q.getQuiz();
            Map<String, Object> quizMap = null;
            if (quiz != null) {
                quizMap = new HashMap<>();
                quizMap.put("_id", quiz.getId());
                quizMap.put("title", quiz.getTitle());
                quizMap.put("description", quiz.getDescription());
                quizMap.put("topic",
                        quiz.getTopic() != null ? quiz.getTopic().getId() : null);
                quizMap.put("duration", quiz.getDuration());
                quizMap.put("questionCount", quiz.getQuestionCount() != null ? quiz.getQuestionCount() : 0);
                quizMap.put("uniqueUserCount", quiz.getUniqueUserCount() != null ? quiz.getUniqueUserCount() : 0L);
                quizMap.put("favoriteCount", quiz.getFavoriteCount() != null ? quiz.getFavoriteCount() : 0L);
                quizMap.put("lastAttemptAt", quiz.getLastAttemptAt());
                quizMap.put("accessTier", quiz.getAccessTier() != null ? quiz.getAccessTier().name() : "FREE");
                quizMap.put("createdAt", quiz.getCreatedAt());
                quizMap.put("updatedAt", quiz.getUpdatedAt());

// tính isLocked giống Mongo: accessTier != FREE
                boolean isLocked = quiz.getAccessTier() != null
                        && quiz.getAccessTier() != Quiz.AccessTier.FREE;

                quizMap.put("isLocked", isLocked);

            }
            qMap.put("quiz", quizMap);

            // ---- answers ----
            List<Map<String, Object>> answerList = q.getAnswers().stream().map(a -> {
                Map<String, Object> aMap = new HashMap<>();
                aMap.put("_id", a.getId());
                aMap.put("text", a.getText());
                aMap.put("isCorrect", a.isCorrect());
                return aMap;
            }).toList();
            qMap.put("answers", answerList);

            return qMap;
        }).toList();

        // 4. Gói lại giống response cũ
        Map<String, Object> result = new HashMap<>();
        result.put("page", page);
        result.put("limit", limit);
        result.put("total", questionPage.getTotalElements());
        result.put("totalPages", questionPage.getTotalPages());
        result.put("data", questionList);

        return result;
    }

    // Lấy chi tiết câu hỏi
    public Map<String, Object> getQuestionByIdSv(String id) {
        if (id == null || id.isBlank()) {
            throw new RuntimeException("ID câu hỏi không hợp lệ");
        }

        Question question = questionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Câu hỏi không tồn tại"));

        return buildQuestionMap(question);
    }

    // helper dùng chung cho create, list, getById
    private Map<String, Object> buildQuestionMap(Question q) {
        Map<String, Object> qMap = new HashMap<>();
        qMap.put("_id", q.getId());
        qMap.put("text", q.getText());
        qMap.put("explanation", q.getExplanation());

        // ---- quiz giống Express ----
        Quiz quiz = q.getQuiz();
        if (quiz != null) {
            Map<String, Object> quizMap = new HashMap<>();
            quizMap.put("_id", quiz.getId());
            quizMap.put("title", quiz.getTitle());
            quizMap.put("description", quiz.getDescription());
            quizMap.put("topic", quiz.getTopic() != null ? quiz.getTopic().getId() : null);
            quizMap.put("uniqueUserCount", quiz.getUniqueUserCount() != null ? quiz.getUniqueUserCount() : 0L);
            quizMap.put("lastAttemptAt", quiz.getLastAttemptAt());
            quizMap.put("favoriteCount", quiz.getFavoriteCount() != null ? quiz.getFavoriteCount() : 0L);
            quizMap.put("duration", quiz.getDuration());
            quizMap.put("questionCount", quiz.getQuestionCount() != null ? quiz.getQuestionCount() : 0);
            quizMap.put("accessTier", quiz.getAccessTier() != null ? quiz.getAccessTier().name() : "FREE");
            quizMap.put("createdAt", quiz.getCreatedAt());
            quizMap.put("updatedAt", quiz.getUpdatedAt());

            boolean isLocked = quiz.getAccessTier() != null
                    && quiz.getAccessTier() != Quiz.AccessTier.FREE;
            quizMap.put("isLocked", isLocked);

            qMap.put("quiz", quizMap);
        } else {
            qMap.put("quiz", null);
        }

        // ---- answers ----
        List<Map<String, Object>> answerList = q.getAnswers().stream().map(a -> {
            Map<String, Object> aMap = new HashMap<>();
            aMap.put("_id", a.getId());
            aMap.put("text", a.getText());
            aMap.put("isCorrect", a.isCorrect());
            return aMap;
        }).toList();

        qMap.put("answers", answerList);

        return qMap;
    }

    // Update câu hỏi
    // Update câu hỏi
    public Map<String, Object> updateQuestionSv(String id, Map<String, Object> body) {

        if (id == null || id.isBlank()) {
            throw new RuntimeException("ID câu hỏi không hợp lệ");
        }

        Question question = questionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Câu hỏi không tồn tại"));

        // ---- Update text ----
        if (body.get("text") != null) {
            question.setText(body.get("text").toString());
        }

        // ---- Update explanation ----
        if (body.get("explanation") != null) {
            question.setExplanation(body.get("explanation").toString());
        }

        // ---- Update answers ----
        if (body.get("answers") != null) {

            List<Map<String, Object>> answersRaw =
                    (List<Map<String, Object>>) body.get("answers");

            if (answersRaw == null || answersRaw.isEmpty()) {
                throw new RuntimeException("Danh sách đáp án không được để trống");
            }

            List<Question.Answer> answers = answersRaw.stream()
                    .map(a -> Question.Answer.builder()
                            // Giữ nguyên id cũ, nếu thiếu thì tạo mới để không bị mất
                            .id(a.get("_id") != null
                                    ? a.get("_id").toString()
                                    : new org.bson.types.ObjectId().toHexString())
                            .text(a.get("text").toString())
                            .isCorrect(Boolean.parseBoolean(a.get("isCorrect").toString()))
                            .build()
                    )
                    .toList();

            question.setAnswers(answers);
        }

        // ---- Update quiz (nếu đổi quiz) ----
        if (body.get("quiz") != null) {
            String quizId = body.get("quiz").toString();
            Quiz quiz = quizRepository.findById(quizId)
                    .orElseThrow(() -> new RuntimeException("Quiz không tồn tại"));
            question.setQuiz(quiz);
        }

        Question saved = questionRepository.save(question);

        return buildQuestionMap(saved);
    }

    public Map<String, Object> deleteQuestionSv(String id) {

        if (id == null || id.isBlank()) {
            throw new RuntimeException("ID câu hỏi không hợp lệ");
        }

        Question question = questionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Câu hỏi không tồn tại"));

        questionRepository.delete(question);

        Map<String, Object> qMap = new HashMap<>();
        qMap.put("_id", question.getId());
        qMap.put("text", question.getText());
        qMap.put("explanation", question.getExplanation());
        qMap.put("answers", question.getAnswers());

        Map<String, Object> quizMap = new HashMap<>();
        Quiz quiz = question.getQuiz();
        if (quiz != null) {
            quizMap.put("_id", quiz.getId());
            quizMap.put("title", quiz.getTitle());
            quizMap.put("description", quiz.getDescription());
        }

        qMap.put("quiz", quizMap);

        return Map.of(
                "message", "Xóa thành công",
                "question", qMap
        );
    }



}
