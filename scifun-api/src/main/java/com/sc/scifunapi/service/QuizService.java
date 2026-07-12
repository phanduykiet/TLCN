package com.sc.scifunapi.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Refresh;
import com.sc.scifunapi.dto.quiz.QuizDetailDTO;
import com.sc.scifunapi.dto.quiz.TopicSimpleDTO;
import com.sc.scifunapi.entity.Quiz;
import com.sc.scifunapi.entity.Quiz.AccessTier;
import com.sc.scifunapi.entity.Subject;
import com.sc.scifunapi.entity.Topic;
import com.sc.scifunapi.repository.QuizRepository;
import com.sc.scifunapi.repository.TopicRepository;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.Operator;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

@Service
@RequiredArgsConstructor
public class QuizService {

    private final QuizRepository quizRepository;
    private final TopicRepository topicRepository;
    private final QuizSearchService quizSearchService;
    private static final String QUIZ_INDEX = "quizzes";

    private final ElasticsearchClient esClient;

    // Thêm Quiz
    public Map<String, Object> createQuizSv(Map<String, Object> data) {

        String title = data.get("title") != null ? data.get("title").toString() : null;
        String description = data.get("description") != null ? data.get("description").toString() : null;
        String topicId = data.get("topic") != null ? data.get("topic").toString() : null;
        Object durationObj = data.get("duration");
        Object questionCountObj = data.get("questionCount");
        Object accessTierObj = data.get("accessTier");

        if (title == null || title.isBlank()) {
            throw new RuntimeException("Tiêu đề quiz không được để trống");
        }
        if (topicId == null || topicId.isBlank()) {
            throw new RuntimeException("Topic ID không được để trống");
        }
        if (durationObj == null) {
            throw new RuntimeException("Thời lượng quiz không được để trống");
        }
        if (questionCountObj == null) {
            throw new RuntimeException("Số câu hỏi không được để trống");
        }

        int duration = Integer.parseInt(durationObj.toString());
        int questionCount = Integer.parseInt(questionCountObj.toString());

        AccessTier accessTier = AccessTier.FREE;
        if (accessTierObj != null) {
            accessTier = AccessTier.valueOf(accessTierObj.toString());
        }

        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new RuntimeException("Topic không tồn tại"));

        Quiz quiz = Quiz.builder()
                .title(title)
                .description(description)
                .topic(topic)
                .duration(duration)
                .questionCount(questionCount)
                .uniqueUserCount(0L)
                .favoriteCount(0L)
                .accessTier(accessTier)
                .build();

        Quiz saved = quizRepository.save(quiz);

        // Sync ES
        quizSearchService.syncOneQuizToES(saved.getId());

        // Trả về giống populate topic -> subject
        Map<String, Object> res = new HashMap<>();
        res.put("_id", saved.getId());
        res.put("title", saved.getTitle());
        res.put("description", saved.getDescription());
        res.put("duration", saved.getDuration());
        res.put("questionCount", saved.getQuestionCount());
        res.put("uniqueUserCount", saved.getUniqueUserCount());
        res.put("favoriteCount", saved.getFavoriteCount());
        res.put("lastAttemptAt", saved.getLastAttemptAt());
        res.put("accessTier", saved.getAccessTier().name());

        Topic t = saved.getTopic();
        if (t != null) {
            Subject subject = t.getSubject();
            Map<String, Object> subjectMap = null;
            if (subject != null) {
                subjectMap = new HashMap<>();
                subjectMap.put("_id", subject.getId());
                subjectMap.put("name", subject.getName());
                subjectMap.put("description", subject.getDescription());
                subjectMap.put("image", subject.getImage());
            }

            Map<String, Object> topicMap = new HashMap<>();
            topicMap.put("_id", t.getId());
            topicMap.put("name", t.getName());
            topicMap.put("description", t.getDescription());
            topicMap.put("subject", subjectMap);

            res.put("topic", topicMap);
        } else {
            res.put("topic", null);
        }

        return res;
    }

    // Lấy danh sách Quiz có phân trang + lọc theo topic + search title
    public Map<String, Object> getQuizzes(
            Integer page,
            Integer limit,
            String topicId,
            String search
    ) {
        try {
            List<Query> must = new ArrayList<>();
            List<Query> filters = new ArrayList<>();

            // lọc theo topicId
            if (topicId != null && !topicId.isBlank()) {
                filters.add(Query.of(q -> q.term(t -> t
                        .field("topic._id")
                        .value(topicId)
                )));
            }

            // search theo title
            if (search != null && !search.isBlank()) {
                String q = search.trim();
                must.add(Query.of(query -> query.match(m -> m
                        .field("title")
                        .query(q)
                        .operator(Operator.And)
                        .fuzziness("AUTO")
                        .minimumShouldMatch("75%")
                )));
            }

            // build query chính
            Query mainQuery;
            if (!must.isEmpty() || !filters.isEmpty()) {
                mainQuery = Query.of(q -> q.bool(b -> {
                    if (!must.isEmpty()) {
                        b = b.must(must);
                    }
                    if (!filters.isEmpty()) {
                        b = b.filter(filters);
                    }
                    return b;
                }));
            } else {
                mainQuery = Query.of(q -> q.matchAll(m -> m));
            }

            Map<String, Object> result = new HashMap<>();

            // Không phân trang -> lấy tất cả
            if (page == null || limit == null) {
                SearchResponse<Map> resp = esClient.search(s -> s
                                .index(QUIZ_INDEX)
                                .size(10_000)              // giống size: 10000
                                .trackTotalHits(t -> t.enabled(true))
                                .query(mainQuery),
                        Map.class
                );

                long total = resp.hits().total() != null
                        ? resp.hits().total().value()
                        : resp.hits().hits().size();

                List<Map<String, Object>> quizzes = new ArrayList<>();
                resp.hits().hits().forEach(hit -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("_id", hit.id());
                    if (hit.source() != null) {
                        //noinspection unchecked
                        map.putAll(hit.source());
                    }
                    quizzes.add(map);
                });

                result.put("page", 1);
                result.put("limit", total);
                result.put("total", total);
                result.put("totalPages", 1);
                result.put("quizzes", quizzes);
                return result;
            }

            // Có phân trang
            int from = (page - 1) * limit;

            SearchResponse<Map> resp = esClient.search(s -> s
                            .index(QUIZ_INDEX)
                            .from(from)
                            .size(limit)
                            .trackTotalHits(t -> t.enabled(true))
                            .query(mainQuery),
                    Map.class
            );

            long total = resp.hits().total() != null
                    ? resp.hits().total().value()
                    : resp.hits().hits().size();

            List<Map<String, Object>> quizzes = new ArrayList<>();
            resp.hits().hits().forEach(hit -> {
                Map<String, Object> map = new HashMap<>();
                map.put("_id", hit.id());
                if (hit.source() != null) {
                    //noinspection unchecked
                    map.putAll(hit.source());
                }
                quizzes.add(map);
            });

            result.put("page", page);
            result.put("limit", limit);
            result.put("total", total);
            result.put("totalPages", (int) Math.ceil(total / (double) limit));
            result.put("quizzes", quizzes);
            return result;

        } catch (IOException e) {
            throw new RuntimeException("Lỗi Elasticsearch: " + e.getMessage(), e);
        }
    }

    // Lấy danh sách quiz thịnh hành
    public Map<String, Object> getTrendingQuizzes(
            int page,
            int limit,
            double timeWeight,
            double popularityWeight,
            String subjectId
    ) {
        int skip = (page - 1) * limit;

        // Lấy tất cả quiz
        List<Quiz> allQuizzes = quizRepository.findAll();

        // Chỉ lấy quiz đã từng có người làm
        Date epoch = new Date(0L);
        List<Quiz> quizzes = allQuizzes.stream()
                .filter(q -> q.getLastAttemptAt() != null && !q.getLastAttemptAt().equals(epoch))
                .filter(q -> {
                    if (subjectId == null || subjectId.isBlank()) return true;
                    if (q.getTopic() == null) return false;
                    Topic topic = q.getTopic();
                    if (topic.getSubject() == null) return false;
                    return subjectId.equals(topic.getSubject().getId());
                })
                .toList();

        if (quizzes.isEmpty()) {
            Map<String, Object> res = new HashMap<>();
            res.put("page", page);
            res.put("limit", limit);
            res.put("total", 0);
            res.put("totalPages", 0);
            res.put("data", List.of());
            return res;
        }

        long now = System.currentTimeMillis();

        // maxTimeDiff
        long maxTimeDiff = quizzes.stream()
                .mapToLong(q -> now - q.getLastAttemptAt().getTime())
                .max()
                .orElse(0L);

        // maxUserCount
        long maxUserCount = quizzes.stream()
                .mapToLong(q -> q.getUniqueUserCount() != null ? q.getUniqueUserCount() : 0L)
                .max()
                .orElse(0L);

        // Tính score cho từng quiz
        class QuizScore {
            Quiz quiz;
            double score;
            double timeScore;
            double popularityScore;

            QuizScore(Quiz quiz, double score, double timeScore, double popularityScore) {
                this.quiz = quiz;
                this.score = score;
                this.timeScore = timeScore;
                this.popularityScore = popularityScore;
            }
        }

        List<QuizScore> scored = new ArrayList<>();

        for (Quiz quiz : quizzes) {
            long timeDiff = now - quiz.getLastAttemptAt().getTime();
            double timeScore = maxTimeDiff > 0 ? 1.0 - (double) timeDiff / maxTimeDiff : 0.0;
            double popularityScore = maxUserCount > 0
                    ? (double) (quiz.getUniqueUserCount() != null ? quiz.getUniqueUserCount() : 0) / maxUserCount
                    : 0.0;

            double finalScore = timeScore * timeWeight + popularityScore * popularityWeight;

            scored.add(new QuizScore(quiz, finalScore, timeScore, popularityScore));
        }

        // sort desc theo score
        scored.sort((a, b) -> Double.compare(b.score, a.score));

        // phân trang
        int end = Math.min(skip + limit, scored.size());
        List<QuizScore> pageItems = skip >= scored.size() ? List.of() : scored.subList(skip, end);

        // map về dạng response như Node
        List<Map<String, Object>> dataList = new ArrayList<>();
        for (QuizScore qs : pageItems) {
            Quiz q = qs.quiz;
            Map<String, Object> item = new HashMap<>();
            item.put("_id", q.getId());
            item.put("title", q.getTitle());
            item.put("description", q.getDescription());
            item.put("duration", q.getDuration());
            item.put("questionCount", q.getQuestionCount());
            item.put("uniqueUserCount", q.getUniqueUserCount());
            item.put("lastAttemptAt", q.getLastAttemptAt());
            item.put("score", Math.round(qs.score * 100.0) / 100.0);
            item.put("accessTier", q.getAccessTier() != null ? q.getAccessTier().name() : "FREE"); // thêm mới

            // Map topic thủ công, tránh bị serialize thừa target/source
            if (q.getTopic() != null) {
                Topic t = q.getTopic();
                Map<String, Object> topicMap = new HashMap<>();
                topicMap.put("id", t.getId());
                topicMap.put("name", t.getName());
                topicMap.put("description", t.getDescription());
                topicMap.put("level", t.getLevel());

                // Map subject trong topic
                if (t.getSubject() != null) {
                    Map<String, Object> subjectMap = new HashMap<>();
                    subjectMap.put("id", t.getSubject().getId());
                    subjectMap.put("name", t.getSubject().getName());
                    subjectMap.put("description", t.getSubject().getDescription());
                    subjectMap.put("image", t.getSubject().getImage());
                    topicMap.put("subject", subjectMap);
                }

                item.put("topic", topicMap);
            } else {
                item.put("topic", null);
            }

            dataList.add(item);
        }
        Map<String, Object> res = new HashMap<>();
        res.put("page", page);
        res.put("limit", limit);
        res.put("total", scored.size());
        res.put("totalPages", (int) Math.ceil(scored.size() / (double) limit));
        res.put("data", dataList);
        return res;
    }

    // Lấy chi tiết quiz
    public QuizDetailDTO getQuizById(String id) {
        if (id == null || id.isBlank()) {
            throw new RuntimeException("ID quiz không hợp lệ");
        }

        Quiz quiz = quizRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Quiz không tồn tại"));

        Topic topic = quiz.getTopic();
        Subject subject = topic.getSubject();

        // TopicSimpleDTO (giống Express)
        TopicSimpleDTO topicDTO = new TopicSimpleDTO(
                topic.getId(),
                topic.getName(),
                topic.getDescription(),
                topic.getLevel(),
                subject != null ? subject.getId() : null
        );

        // QuizDetailDTO
        return new QuizDetailDTO(
                quiz.getId(),
                quiz.getTitle(),
                quiz.getDescription(),
                topicDTO,
                quiz.getUniqueUserCount(),
                quiz.getLastAttemptAt(),
                quiz.getFavoriteCount(),
                quiz.getDuration(),
                quiz.getQuestionCount(),
                quiz.getAccessTier().name(),
                quiz.getCreatedAt(),
                quiz.getUpdatedAt(),
                // isLocked = true nếu accessTier != FREE
                !quiz.getAccessTier().name().equalsIgnoreCase("FREE")
        );
    }

    // cập nhật quiz
    public Map<String, Object> updateQuizSv(String id, Map<String, Object> data) {
        if (id == null || id.isBlank()) {
            throw new RuntimeException("ID quiz không hợp lệ");
        }

        Quiz quiz = quizRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Quiz không tồn tại"));

        // --- Cập nhật từng field nếu có trong body ---

        // title
        if (data.containsKey("title")) {
            String title = data.get("title") != null ? data.get("title").toString() : null;
            if (title == null || title.isBlank()) {
                throw new RuntimeException("Tiêu đề quiz không được để trống");
            }
            quiz.setTitle(title);
        }

        // description
        if (data.containsKey("description")) {
            String description = data.get("description") != null ? data.get("description").toString() : null;
            quiz.setDescription(description);
        }

        // topic
        if (data.containsKey("topic")) {
            String topicId = data.get("topic") != null ? data.get("topic").toString() : null;
            if (topicId == null || topicId.isBlank()) {
                throw new RuntimeException("Topic ID không được để trống");
            }
            Topic topic = topicRepository.findById(topicId)
                    .orElseThrow(() -> new RuntimeException("Topic không tồn tại"));
            quiz.setTopic(topic);
        }

        // duration
        if (data.containsKey("duration")) {
            Object durationObj = data.get("duration");
            if (durationObj == null) {
                throw new RuntimeException("Thời lượng quiz không được để trống");
            }
            int duration = Integer.parseInt(durationObj.toString());
            if (duration <= 0) {
                throw new RuntimeException("Thời lượng quiz phải lớn hơn 0");
            }
            quiz.setDuration(duration);
        }

        // accessTier
        if (data.containsKey("accessTier")) {
            Object accessTierObj = data.get("accessTier");
            if (accessTierObj != null) {
                AccessTier tier = AccessTier.valueOf(accessTierObj.toString());
                quiz.setAccessTier(tier);
            }
        }

         if (data.containsKey("questionCount")) {
             int qCount = Integer.parseInt(data.get("questionCount").toString());
             quiz.setQuestionCount(qCount);
         }

        Quiz saved = quizRepository.save(quiz);

        // Sync ES
        quizSearchService.syncOneQuizToES(saved.getId());

        // Build response giống createQuizSv
        Map<String, Object> res = new HashMap<>();
        res.put("_id", saved.getId());
        res.put("title", saved.getTitle());
        res.put("description", saved.getDescription());
        res.put("duration", saved.getDuration());
        res.put("questionCount", saved.getQuestionCount());
        res.put("uniqueUserCount", saved.getUniqueUserCount());
        res.put("favoriteCount", saved.getFavoriteCount());
        res.put("lastAttemptAt", saved.getLastAttemptAt());
        res.put("accessTier", saved.getAccessTier().name());

        Topic t = saved.getTopic();
        if (t != null) {
            Subject subject = t.getSubject();
            Map<String, Object> subjectMap = null;
            if (subject != null) {
                subjectMap = new HashMap<>();
                subjectMap.put("_id", subject.getId());
                subjectMap.put("name", subject.getName());
                subjectMap.put("description", subject.getDescription());
                subjectMap.put("image", subject.getImage());
            }

            Map<String, Object> topicMap = new HashMap<>();
            topicMap.put("_id", t.getId());
            topicMap.put("name", t.getName());
            topicMap.put("description", t.getDescription());
            topicMap.put("subject", subjectMap);

            res.put("topic", topicMap);
        } else {
            res.put("topic", null);
        }

        return res;
    }

    //xóa quiz
    // Xóa Quiz
    public Map<String, Object> deleteQuizSv(String id) {

        if (id == null || id.isBlank()) {
            throw new RuntimeException("ID quiz không hợp lệ");
        }

        Quiz quiz = quizRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Quiz không tồn tại"));

        quizRepository.deleteById(id);

        // Xóa khỏi Elasticsearch
        quizSearchService.deleteOneQuizFromES(id);

        Map<String, Object> res = new HashMap<>();
        res.put("message", "Xóa thành công");
        res.put("quiz", quiz.getId());

        return res;
    }

    // ✅ Xóa toàn bộ & sync lại tất cả quiz từ DB lên ES
    public void reindexAllQuizzes() {
        // 1. Xoá toàn bộ document trong index "quizzes"
        try {
            esClient.deleteByQuery(b -> b
                    .index(QUIZ_INDEX)
                    .query(q -> q.matchAll(m -> m))
                    .refresh(true)
            );
        } catch (IOException e) {
            throw new RuntimeException("Elasticsearch deleteAll error: " + e.getMessage());
        }

        // 2. Lấy toàn bộ quiz từ DB
        Iterable<Quiz> allQuizzes = quizRepository.findAll();

        // 3. Index lại từng quiz
        for (Quiz quiz : allQuizzes) {
            Map<String, Object> doc = quizSearchService.buildDocFromQuiz(quiz);
            try {
                esClient.index(i -> i
                        .index(QUIZ_INDEX)
                        .id(quiz.getId())
                        .document(doc)
                        .refresh(Refresh.True)
                );
            } catch (IOException e) {
                throw new RuntimeException("Elasticsearch index error for quizId=" + quiz.getId());
            }
        }
    }

}
