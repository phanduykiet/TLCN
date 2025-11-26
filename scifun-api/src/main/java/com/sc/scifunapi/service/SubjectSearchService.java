package com.sc.scifunapi.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.sc.scifunapi.entity.Subject;
import com.sc.scifunapi.repository.SubjectRepository;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SubjectSearchService {

    private final ElasticsearchClient esClient;
    private final SubjectRepository subjectRepository;

    private static final String SUBJECT_INDEX = "subjects";

    // Sync 1 subject lên ES (create/update)
    public void syncOneSubjectToES(String subjectId) {
        Subject s = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new RuntimeException("Subject không tồn tại"));

        Map<String, Object> doc = new HashMap<>();
        doc.put("name", s.getName());
        doc.put("description", s.getDescription() != null ? s.getDescription() : "");
        doc.put("maxTopics", s.getMaxTopics() != null ? s.getMaxTopics() : 20);
        doc.put("image", s.getImage() != null ? s.getImage()
                : "https://res.cloudinary.com/dglm2f7sr/image/upload/v1761400287/default_gdfbhs.png");

        try {
            esClient.index(i -> i
                    .index(SUBJECT_INDEX)
                    .id(subjectId)
                    .document(doc)
                    .refresh(co.elastic.clients.elasticsearch._types.Refresh.True)
            );
        } catch (IOException e) {
            throw new RuntimeException("Elasticsearch index error");
        }
    }

    // Xoá 1 subject khỏi ES
    public void deleteOneSubjectFromES(String subjectId) {
        try {
            esClient.delete(d -> d
                    .index(SUBJECT_INDEX)
                    .id(subjectId)
                    .refresh(co.elastic.clients.elasticsearch._types.Refresh.True)
            );
        } catch (IOException e) {
            throw new RuntimeException("Elasticsearch delete error");
        }
    }


}
