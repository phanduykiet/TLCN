package com.sc.scifunapi.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Refresh;
import com.sc.scifunapi.entity.Subject;
import com.sc.scifunapi.entity.Topic;
import com.sc.scifunapi.repository.SubjectRepository;
import com.sc.scifunapi.repository.TopicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TopicSearchService {

    private final ElasticsearchClient esClient;
    private final TopicRepository topicRepository;
    private final SubjectRepository subjectRepository;

    private static final String TOPIC_INDEX = "topics";

    // Dùng chung logic build doc từ Topic
    public Map<String, Object> buildDocFromTopic(Topic topic) {
        Subject subject = topic.getSubject(); // có thể null

        Map<String, Object> subjectMap = null;
        if (subject != null) {
            subjectMap = new HashMap<>();
            subjectMap.put("_id", subject.getId());
            subjectMap.put("name", subject.getName() != null ? subject.getName() : "");
            subjectMap.put("description", subject.getDescription() != null ? subject.getDescription() : "");
            subjectMap.put("image", subject.getImage() != null ? subject.getImage() : "");
        }

        Map<String, Object> doc = new HashMap<>();
        doc.put("name", topic.getName());
        doc.put("description", topic.getDescription() != null ? topic.getDescription() : "");
        doc.put("level", topic.getLevel());
        doc.put("subject", subjectMap);

        return doc;
    }

    // Sync 1 topic lên ES (dùng cho create và update)
    public void syncOneTopicToES(String topicId) {
        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new RuntimeException("Topic không tồn tại"));

        // Vì Topic.subject là @DBRef Subject nên lấy trực tiếp
        Subject subject = topic.getSubject();  // có thể null

        Map<String, Object> doc = new HashMap<>();
        doc.put("name", topic.getName());
        doc.put("description",
                topic.getDescription() != null ? topic.getDescription() : "");
        doc.put("level", topic.getLevel());

        doc.put("subject", subject == null ? null : Map.of(
                "_id", subject.getId(),
                "name", subject.getName() != null ? subject.getName() : "",
                "description", subject.getDescription() != null ? subject.getDescription() : "",
                "image", subject.getImage() != null ? subject.getImage() : ""
        ));

        try {
            esClient.index(i -> i
                    .index(TOPIC_INDEX)
                    .id(topicId)
                    .document(doc)
                    .refresh(co.elastic.clients.elasticsearch._types.Refresh.True)
            );
        } catch (IOException e) {
            throw new RuntimeException("Elasticsearch index error");
        }
    }

    // Xóa 1 topic khỏi ES
    public void deleteOneTopicFromES(String topicId) {
        try {
            esClient.delete(d -> d
                    .index(TOPIC_INDEX)
                    .id(topicId)
                    .refresh(Refresh.True)
            );
        } catch (IOException e) {
            throw new RuntimeException("Elasticsearch delete error");
        }
    }

}
