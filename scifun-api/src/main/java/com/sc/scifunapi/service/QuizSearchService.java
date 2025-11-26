package com.sc.scifunapi.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Refresh;
import com.sc.scifunapi.entity.Quiz;
import com.sc.scifunapi.entity.Subject;
import com.sc.scifunapi.entity.Topic;
import com.sc.scifunapi.repository.QuizRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class QuizSearchService {

    private final ElasticsearchClient esClient;
    private final QuizRepository quizRepository;

    private static final String QUIZ_INDEX = "quizzes";

    // Sync 1 quiz lên ES
    public void syncOneQuizToES(String quizId) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz không tồn tại"));

        Topic topic = quiz.getTopic();
        Subject subject = (topic != null) ? topic.getSubject() : null;

        Map<String, Object> subjectMap = null;
        if (subject != null) {
            subjectMap = new HashMap<>();
            subjectMap.put("_id", subject.getId());
            subjectMap.put("name", subject.getName() != null ? subject.getName() : "");
            subjectMap.put("description", subject.getDescription() != null ? subject.getDescription() : "");
            subjectMap.put("image", subject.getImage() != null ? subject.getImage() : "");
        }

        Map<String, Object> topicMap = null;
        if (topic != null) {
            topicMap = new HashMap<>();
            topicMap.put("_id", topic.getId());
            topicMap.put("name", topic.getName() != null ? topic.getName() : "");
            topicMap.put("description", topic.getDescription() != null ? topic.getDescription() : "");
            topicMap.put("subject", subjectMap);
        }

        Map<String, Object> doc = new HashMap<>();
        doc.put("title", quiz.getTitle());
        doc.put("description", quiz.getDescription() != null ? quiz.getDescription() : "");
        doc.put("duration", quiz.getDuration());
        doc.put("questionCount", quiz.getQuestionCount() != null ? quiz.getQuestionCount() : 0);
        doc.put("uniqueUserCount", quiz.getUniqueUserCount() != null ? quiz.getUniqueUserCount() : 0L);
        doc.put("favoriteCount", quiz.getFavoriteCount() != null ? quiz.getFavoriteCount() : 0L);
        doc.put("lastAttemptAt", quiz.getLastAttemptAt() != null ? quiz.getLastAttemptAt() : new Date(0));
        doc.put("accessTier", quiz.getAccessTier() != null ? quiz.getAccessTier().name() : "FREE");
        doc.put("topic", topicMap);

        try {
            esClient.index(i -> i
                    .index(QUIZ_INDEX)
                    .id(quizId)
                    .document(doc)
                    .refresh(Refresh.True)
            );
        } catch (IOException e) {
            throw new RuntimeException("Elasticsearch index error");
        }
    }

    // Xóa 1 quiz khỏi ES
    public void deleteOneQuizFromES(String quizId) {
        try {
            esClient.delete(d -> d
                    .index(QUIZ_INDEX)
                    .id(quizId)
                    .refresh(co.elastic.clients.elasticsearch._types.Refresh.True)
            );
        } catch (IOException e) {
            throw new RuntimeException("Elasticsearch delete error");
        }
    }


}
