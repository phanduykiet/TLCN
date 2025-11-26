package com.sc.scifunapi.service;

import com.sc.scifunapi.entity.FavoriteQuiz;
import com.sc.scifunapi.entity.Quiz;
import com.sc.scifunapi.entity.Topic;
import com.sc.scifunapi.repository.FavoriteQuizRepository;
import com.sc.scifunapi.repository.QuizRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FavoriteQuizService {

    private final FavoriteQuizRepository favoriteQuizRepository;
    private final QuizRepository quizRepository;

    // Thêm vào yêu thích (tương đương addFavoriteQuizSv bên Express)
    public FavoriteQuiz addFavoriteQuiz(String userId, String quizId) {

        try {
            // Tạo bản ghi favorite
            FavoriteQuiz favorite = FavoriteQuiz.builder()
                    .user(userId)
                    .quiz(quizId)
                    .createdAt(new Date())
                    .build();

            FavoriteQuiz saved = favoriteQuizRepository.save(favorite);

            // Tăng favoriteCount của Quiz
            quizRepository.findById(quizId).ifPresent(q -> {
                Long current = q.getFavoriteCount() != null ? q.getFavoriteCount() : 0L;
                q.setFavoriteCount(current + 1);
                quizRepository.save(q);
            });

            return saved;

        } catch (DuplicateKeyException e) {
            // Ném lại cho controller bắt và trả message giống Express
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Không thể thêm vào yêu thích: " + e.getMessage(), e);
        }
    }

    // Bỏ yêu thích
    public boolean removeFavoriteQuiz(String userId, String quizId) {
        try {
            Optional<FavoriteQuiz> favoriteOpt =
                    favoriteQuizRepository.findByUserAndQuiz(userId, quizId);

            if (favoriteOpt.isEmpty()) {
                return false; // giống deletedCount === 0
            }

            // Xoá bản ghi yêu thích
            favoriteQuizRepository.delete(favoriteOpt.get());

            // Giảm favoriteCount (không để âm)
            quizRepository.findById(quizId).ifPresent(q -> {
                Long current = q.getFavoriteCount() != null ? q.getFavoriteCount() : 0L;
                long newValue = current > 0 ? current - 1 : 0;
                q.setFavoriteCount(newValue);
                quizRepository.save(q);
            });

            return true;

        } catch (Exception e) {
            throw new RuntimeException("Không thể bỏ yêu thích: " + e.getMessage(), e);
        }
    }

    // Lấy danh sách quiz yêu thích với phân trang + filter topicId (nếu có)
    public Map<String, Object> getFavoriteQuizzes(
            String userId,
            int page,
            int limit,
            String topicId
    ) {
        if (page < 1) page = 1;
        if (limit < 1) limit = 10;

        Pageable pageable = PageRequest.of(page - 1, limit, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<FavoriteQuiz> favoritePage;
        long total;

        // Nếu có topicId → lọc quiz trước
        if (topicId != null && !topicId.isBlank()) {
            List<Quiz> quizzesByTopic = quizRepository.findByTopic_Id(topicId);
            if (quizzesByTopic.isEmpty()) {
                // Không có quiz nào trong topic → trả về rỗng
                return Map.of(
                        "page", page,
                        "limit", limit,
                        "total", 0,
                        "totalPages", 0,
                        "data", List.of()
                );
            }

            List<String> quizIds = quizzesByTopic.stream()
                    .map(Quiz::getId)
                    .collect(Collectors.toList());

            favoritePage = favoriteQuizRepository.findByUserAndQuizIn(userId, quizIds, pageable);
            total = favoriteQuizRepository.countByUserAndQuizIn(userId, quizIds);

        } else {
            favoritePage = favoriteQuizRepository.findByUser(userId, pageable);
            total = favoriteQuizRepository.countByUser(userId);
        }

        List<FavoriteQuiz> favorites = favoritePage.getContent();

        // Lấy danh sách quizId để query 1 lần
        List<String> quizIds = favorites.stream()
                .map(FavoriteQuiz::getQuiz)
                .distinct()
                .toList();

        Map<String, Quiz> quizMap = quizRepository.findAllById(quizIds)
                .stream()
                .collect(Collectors.toMap(Quiz::getId, q -> q));

        // Build data giống Express: favorite + quiz (kèm topic name)
        List<Map<String, Object>> data = new ArrayList<>();

        for (FavoriteQuiz fav : favorites) {
            Quiz q = quizMap.get(fav.getQuiz());
            if (q == null) {
                // quiz bị xoá thì bỏ qua
                continue;
            }

            Map<String, Object> quizJson = new HashMap<>();
            quizJson.put("_id", q.getId());
            quizJson.put("title", q.getTitle());
            quizJson.put("description", q.getDescription());
            quizJson.put("duration", q.getDuration());
            quizJson.put("questionCount", q.getQuestionCount());
            quizJson.put("uniqueUserCount", q.getUniqueUserCount());
            quizJson.put("favoriteCount", q.getFavoriteCount());
            quizJson.put("lastAttemptAt", q.getLastAttemptAt());
            quizJson.put("accessTier", q.getAccessTier() != null ? q.getAccessTier().name() : "FREE");
            quizJson.put("createdAt", q.getCreatedAt());
            quizJson.put("updatedAt", q.getUpdatedAt());

            Topic t = q.getTopic();
            if (t != null) {
                Map<String, Object> topicJson = new HashMap<>();
                topicJson.put("_id", t.getId());
                topicJson.put("name", t.getName());
                quizJson.put("topic", topicJson);
            } else {
                quizJson.put("topic", null);
            }

            Map<String, Object> favJson = new HashMap<>();
            favJson.put("_id", fav.getId());
            favJson.put("user", fav.getUser());
            favJson.put("quiz", quizJson);
            favJson.put("createdAt", fav.getCreatedAt());

            data.add(favJson);
        }

        long totalPages = (long) Math.ceil((double) total / limit);

        Map<String, Object> result = new HashMap<>();
        result.put("page", page);
        result.put("limit", limit);
        result.put("total", total);
        result.put("totalPages", totalPages);
        result.put("data", data);

        return result;
    }
}
