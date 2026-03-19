package com.sc.scifunapi.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.sc.scifunapi.entity.Subject;
import com.sc.scifunapi.entity.Topic;
import com.sc.scifunapi.repository.SubjectRepository;
import com.sc.scifunapi.repository.TopicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.Operator;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TopicService {

    private final TopicRepository topicRepository;
    private final SubjectRepository subjectRepository;
    private final TopicSearchService topicSearchService;
    private final ElasticsearchClient esClient;

    private static final String TOPIC_INDEX = "topics";

    // Thêm Topic
    public Map<String, Object> createTopicSv(Map<String, Object> data) {

        String name = data.get("name") != null ? data.get("name").toString() : null;
        String description = data.get("description") != null ? data.get("description").toString() : null;
        String level = data.get("level") != null ? data.get("level").toString() : null;
        String subjectId = data.get("subject") != null ? data.get("subject").toString() : null;

        if (subjectId == null) {
            throw new RuntimeException("Subject ID không được để trống");
        }

        // Lấy subject từ DB
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new RuntimeException("Subject không tồn tại"));

        // Tạo topic
        Topic topic = Topic.builder()
                .name(name)
                .description(description)
                .level(level)
                .subject(subject)   // <-- GÁN SUBJECT ENTITY, KHÔNG PHẢI STRING
                .build();

        Topic saved = topicRepository.save(topic);

        // Xây response giống NodeJS (populate)
        Map<String, Object> res = new HashMap<>();
        res.put("_id", saved.getId());
        res.put("name", saved.getName());
        res.put("description", saved.getDescription());
        res.put("level", saved.getLevel());
        res.put("subject", Map.of(
                "_id", subject.getId(),
                "name", subject.getName(),
                "description", subject.getDescription(),
                "image", subject.getImage()
        ));

        // Sync ES
        topicSearchService.syncOneTopicToES(saved.getId());

        return res;
    }

    // Lấy danh sách phân trang tìm kiếm
    public Map<String, Object> getTopics(
            Integer page,
            Integer limit,
            String subjectId,
            String search
    ) {
        try {
            List<Query> must = new ArrayList<>();
            List<Query> filters = new ArrayList<>();

            // filter theo subjectId
            if (subjectId != null && !subjectId.isBlank()) {
                filters.add(Query.of(q -> q
                        .term(t -> t
                                .field("subject._id")
                                .value(subjectId)
                        )
                ));
            }

            // search theo name
            if (search != null && !search.isBlank()) {
                must.add(Query.of(q -> q
                        .match(m -> m
                                .field("name")
                                .query(search.trim())
                                .operator(Operator.And)
                                .fuzziness("AUTO")
                                .minimumShouldMatch("75%")
                        )
                ));
            }

            // build bool query giống Node
            Query boolQuery = Query.of(q -> q.bool(b -> {
                if (must.isEmpty()) {
                    b.must(Query.of(q2 -> q2.matchAll(m -> m)));
                } else {
                    b.must(must);
                }
                if (!filters.isEmpty()) {
                    b.filter(filters);
                }
                return b;
            }));

            // không phân trang → lấy tất
            if (page == null || limit == null) {
                SearchResponse<Map> resp = esClient.search(
                        s -> s.index(TOPIC_INDEX)
                                .size(10_000)
                                .trackTotalHits(t -> t.enabled(true))
                                .query(boolQuery),
                        Map.class
                );

                long total = resp.hits().total() != null
                        ? resp.hits().total().value()
                        : resp.hits().hits().size();

                List<Map<String, Object>> topics = resp.hits().hits().stream()
                        .map(hit -> {
                            Map src = hit.source(); // raw Map
                            Map<String, Object> doc = new HashMap<>();
                            if (src != null) {
                                doc.putAll(src);
                            }
                            doc.put("_id", hit.id());
                            return doc;
                        })
                        .toList();

                Map<String, Object> result = new HashMap<>();
                result.put("page", 1);
                result.put("limit", total);
                result.put("total", total);
                result.put("totalPages", 1);
                result.put("topics", topics);
                return result;
            }


            // có phân trang
            int from = Math.max(0, (page - 1) * limit);

            SearchResponse<Map> resp = esClient.search(
                    s -> s.index(TOPIC_INDEX)
                            .from(from)
                            .size(limit)
                            .trackTotalHits(t -> t.enabled(true))
                            .query(boolQuery),
                    Map.class
            );

            long total = resp.hits().total() != null
                    ? resp.hits().total().value()
                    : resp.hits().hits().size();

            List<Map<String, Object>> topics = resp.hits().hits().stream()
                    .map(hit -> {
                        Map src = hit.source();
                        Map<String, Object> doc = new HashMap<>();
                        if (src != null) {
                            doc.putAll(src);
                        }
                        doc.put("_id", hit.id());
                        return doc;
                    })
                    .toList();

            Map<String, Object> result = new HashMap<>();
            result.put("page", page);
            result.put("limit", limit);
            result.put("total", total);
            result.put("totalPages", (int) Math.ceil(total / (double) limit));
            result.put("topics", topics);
            return result;


        } catch (IOException e) {
            throw new RuntimeException("Lỗi Elasticsearch: " + e.getMessage(), e);
        }
    }

    // helper: convert Hit -> Map có thêm _id
    private Map<String, Object> hitToDoc(Hit<Map<String, Object>> hit) {
        Map<String, Object> src = hit.source();
        if (src == null) src = new HashMap<>();
        Map<String, Object> doc = new HashMap<>(src);
        doc.put("_id", hit.id());
        return doc;
    }

    // Lấy chi tiết chủ đề
    public Map<String, Object> getTopicById(String id) {
        if (id == null || id.isBlank()) {
            throw new RuntimeException("ID topic không hợp lệ");
        }

        Topic topic = topicRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Topic không tồn tại"));

        Subject subject = topic.getSubject(); // DBRef

        Map<String, Object> res = new HashMap<>();
        res.put("_id", topic.getId());
        res.put("name", topic.getName());
        res.put("description", topic.getDescription());
        res.put("level", topic.getLevel());

        if (subject != null) {
            Map<String, Object> subjectMap = new HashMap<>();
            subjectMap.put("_id", subject.getId());
            subjectMap.put("name", subject.getName());
            subjectMap.put("description", subject.getDescription());
            subjectMap.put("image", subject.getImage());
            res.put("subject", subjectMap);
        } else {
            res.put("subject", null);
        }

        return res;
    }

    // Cập nhật chủ đề
    public Map<String, Object> updateTopic(String id, Map<String, Object> data) {
        if (id == null || id.isBlank()) {
            throw new RuntimeException("ID topic không hợp lệ");
        }

        Topic topic = topicRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Topic không tồn tại"));

        // update name
        if (data.get("name") != null) {
            topic.setName(data.get("name").toString());
        }

        // update description
        if (data.get("description") != null) {
            topic.setDescription(data.get("description").toString());
        }

        if (data.get("level") != null) {
            topic.setLevel(data.get("level").toString());
        }

        // update subject (nhận id subject)
        if (data.get("subject") != null) {
            String subjectId = data.get("subject").toString();
            Subject subject = subjectRepository.findById(subjectId)
                    .orElseThrow(() -> new RuntimeException("Subject không tồn tại"));
            topic.setSubject(subject);   // DBRef như em đang dùng
        }

        Topic saved = topicRepository.save(topic);

        // Sync lên ES
        topicSearchService.syncOneTopicToES(saved.getId());

        // Trả về format tương tự populate("subject")
        Map<String, Object> res = new HashMap<>();
        res.put("_id", saved.getId());
        res.put("name", saved.getName());
        res.put("description", saved.getDescription());
        res.put("level", saved.getLevel());

        Subject subject = saved.getSubject();
        if (subject != null) {
            Map<String, Object> subjectMap = new HashMap<>();
            subjectMap.put("_id", subject.getId());
            subjectMap.put("name", subject.getName());
            subjectMap.put("description", subject.getDescription());
            subjectMap.put("image", subject.getImage());
            res.put("subject", subjectMap);
        } else {
            res.put("subject", null);
        }

        return res;
    }

    // Xóa Topic
    public void deleteTopic(String id) {
        if (id == null || id.isBlank()) {
            throw new RuntimeException("ID topic không hợp lệ");
        }

        Topic topic = topicRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Topic không tồn tại"));

        // Xóa trong Mongo
        topicRepository.delete(topic);

        // Xóa trong Elasticsearch
        topicSearchService.deleteOneTopicFromES(id);
    }

    // ✅ Xóa toàn bộ & sync lại tất cả topic từ DB lên ES
    public void reindexAllTopics() {
        // 1. Xóa toàn bộ document trong index "topics"
        try {
            esClient.deleteByQuery(b -> b
                    .index(TOPIC_INDEX)
                    .query(q -> q.matchAll(m -> m))
                    .refresh(true)
            );
        } catch (IOException e) {
            throw new RuntimeException("Elasticsearch deleteAll error: " + e.getMessage());
        }

        // 2. Lấy toàn bộ topic từ DB
        Iterable<Topic> allTopics = topicRepository.findAll();

        // 3. Index lại từng topic
        for (Topic topic : allTopics) {
            Map<String, Object> doc = topicSearchService.buildDocFromTopic(topic);
            try {
                esClient.index(i -> i
                        .index(TOPIC_INDEX)
                        .id(topic.getId())
                        .document(doc)
                        .refresh(co.elastic.clients.elasticsearch._types.Refresh.True)
                );
            } catch (IOException e) {
                throw new RuntimeException("Elasticsearch index error for topicId=" + topic.getId());
            }
        }
    }

}
