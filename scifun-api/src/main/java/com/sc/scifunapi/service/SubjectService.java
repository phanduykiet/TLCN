package com.sc.scifunapi.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.sc.scifunapi.entity.Subject;
import com.sc.scifunapi.repository.SubjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SubjectService {

    private final SubjectRepository subjectRepository;
    private final CloudinaryService cloudinaryService;
    private final SubjectSearchService subjectSearchService;
    private final ElasticsearchClient esClient;

    private static final String SUBJECT_INDEX = "subjects";

    // Tạo môn học
    public Subject createSubjectSv(Map<String, String> data, MultipartFile image) {
        // upload ảnh nếu có
        if (image != null && !image.isEmpty()) {
            String url = cloudinaryService.uploadImage(image, "Subject");
            data.put("image", url);
        }

        // map field
        String name = data.get("name");
        String description = data.getOrDefault("description", null);
        Integer maxTopics = null;
        try { maxTopics = data.get("maxTopics") != null ? Integer.parseInt(data.get("maxTopics")) : null; }
        catch (NumberFormatException ignored) {}

        Subject subject = Subject.builder()
                .name(name)
                .description(description)
                .maxTopics(maxTopics != null ? maxTopics : 20)
                .image(data.get("image")) // nếu null → entity dùng default
                .build();

        Subject saved = subjectRepository.save(subject);

        // Sync lên ES
        subjectSearchService.syncOneSubjectToES(saved.getId());

        return saved;
    }

    // Lấy chi tiết môn học
    public Subject getSubjectByIdSv(String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new RuntimeException("ID môn học không hợp lệ");
        }

        return subjectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Môn học không tồn tại"));
    }

    // Cập nhật môn học
    public Subject updateSubjectSv(String id, Map<String, String> data, MultipartFile image) {
        if (id == null || id.isBlank()) {
            throw new RuntimeException("ID subject không hợp lệ");
        }

        var subject = subjectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Subject không tồn tại"));

        // upload ảnh nếu có
        if (image != null && !image.isEmpty()) {
            String url = cloudinaryService.uploadImage(image, "Subject");
            data.put("image", url);
        }

        // cập nhật các field nếu gửi lên
        if (data.containsKey("name")) subject.setName(data.get("name"));
        if (data.containsKey("description")) subject.setDescription(data.get("description"));
        if (data.containsKey("maxTopics")) {
            try {
                subject.setMaxTopics(Integer.parseInt(data.get("maxTopics")));
            } catch (NumberFormatException ignored) {}
        }
        if (data.containsKey("image")) subject.setImage(data.get("image"));

        var saved = subjectRepository.save(subject);

        // Sync lên ES
        subjectSearchService.syncOneSubjectToES(saved.getId());

        return saved;
    }

    // Xóa môn học
    public void deleteSubjectSv(String id) {
        if (id == null || id.isBlank()) {
            throw new RuntimeException("ID subject không hợp lệ");
        }

        var subject = subjectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Subject không tồn tại"));

        subjectRepository.deleteById(subject.getId());

        // Xoá khỏi ES
        subjectSearchService.deleteOneSubjectFromES(id);
    }

    // Lấy danh sách môn học với phân trang + tìm kiếm
    public Map<String, Object> getSubjectsSv(Integer page, Integer limit, String search) {
        try {
            Query query;

            if (search != null && !search.trim().isEmpty()) {
                query = BoolQuery.of(b -> b
                        .must(MultiMatchQuery.of(m -> m
                                .query(search.trim())
                                .fields("name^2", "description", "code")
                                .operator(Operator.And)
                                .fuzziness("AUTO")
                                .minimumShouldMatch("75%")
                        )._toQuery())
                )._toQuery();
            } else {
                query = MatchAllQuery.of(m -> m)._toQuery();
            }

            int from = (page != null && limit != null) ? (page - 1) * limit : 0;
            int size = (limit != null) ? limit : 10000;

            SearchResponse<Map> result = esClient.search(s -> s
                            .index(SUBJECT_INDEX)
                            .query(query)
                            .from(from)
                            .size(size)
                            .trackTotalHits(t -> t.enabled(true)),
                    Map.class
            );

            long total = result.hits().total() != null ? result.hits().total().value() : 0;

            List<Map<String, Object>> subjects = new ArrayList<>();
            for (Hit<Map> hit : result.hits().hits()) {
                Map<String, Object> src = new HashMap<>(hit.source());
                src.put("_id", hit.id());
                subjects.add(src);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("page", page != null ? page : 1);
            response.put("limit", limit != null ? limit : total);
            response.put("total", total);
            response.put("totalPages", limit != null ? (int) Math.ceil((double) total / limit) : 1);
            response.put("subjects", subjects);

            return response;
        } catch (IOException e) {
            throw new RuntimeException("Lỗi khi truy vấn Elasticsearch: " + e.getMessage());
        }
    }
}
