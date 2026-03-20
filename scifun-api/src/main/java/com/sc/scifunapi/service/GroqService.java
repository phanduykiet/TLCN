package com.sc.scifunapi.service;

import com.sc.scifunapi.entity.Subject;
import com.sc.scifunapi.entity.Topic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class GroqService {

    @Value("${app.groq.api-key}")
    private String apiKey;

    @Value("${app.groq.url}")
    private String apiUrl;

    @Value("${app.groq.model}")
    private String model;

    private final RestTemplate restTemplate = new RestTemplate();

    public String chat(
            String userMessage,
            String fullname,
            int age,
            String ageGroup,
            String subject,
            String level,
            List<Subject> subjects,
            List<Topic> topics,
            List<Map<String, Object>> relatedQuestions,
            List<Map<String, String>> history
    ) {
        // Giữ nguyên phần build subjectList
        String subjectList = subjects.stream()
                .map(s -> "- " + s.getName() + ": " + s.getDescription())
                .collect(Collectors.joining("\n"));

        // Giữ nguyên phần build topicList
        String topicList = topics.stream()
                .collect(Collectors.groupingBy(
                        t -> t.getLevel() != null ? t.getLevel() : "Chưa phân loại"
                ))
                .entrySet().stream()
                .map(entry -> {
                    String lvl = entry.getKey();
                    String topicNames = entry.getValue().stream()
                            .map(t -> t.getName() +
                                    (t.getSubject() != null ? " (" + t.getSubject().getName() + ")" : ""))
                            .collect(Collectors.joining(", "));
                    return "  [" + lvl + "]: " + topicNames;
                })
                .collect(Collectors.joining("\n"));

        // Giữ nguyên phần build questionContext
        String questionContext = "";
        if (!relatedQuestions.isEmpty()) {
            String[] keywords = userMessage.toLowerCase().split("\\s+");
            List<Map<String, Object>> filtered = relatedQuestions.stream()
                    .filter(q -> {
                        String qText = q.get("text").toString().toLowerCase();
                        for (String kw : keywords) {
                            if (kw.length() > 3 && qText.contains(kw)) return true;
                        }
                        return false;
                    })
                    .toList();

            if (!filtered.isEmpty()) {
                questionContext = "\n=== CÂU HỎI LIÊN QUAN TRONG HỆ THỐNG ===\n" +
                        filtered.stream()
                                .map(q -> "- Câu hỏi: " + q.get("text") +
                                        "\n  Đáp án đúng: " + q.get("correctAnswer") +
                                        (q.get("explanation") != null && !q.get("explanation").toString().isBlank()
                                                ? "\n  Giải thích: " + q.get("explanation") : ""))
                                .collect(Collectors.joining("\n")) +
                        "\nHãy ưu tiên dựa vào các câu hỏi trên để trả lời.";
            }
        }

        boolean hasRelated = !questionContext.isBlank();

        // Giữ nguyên systemPrompt
        String systemPrompt = """
        Bạn là trợ lý học tập thông minh của ứng dụng SciFun.
        SciFun là app học Lý, Hóa, Sinh theo phong cách Duolingo.
        
        === THÔNG TIN HỌC SINH ===
        - Tên: %s
        - Tuổi: %d (nhóm tuổi: %s)
        - Môn học yêu thích: %s
        - Trình độ hiện tại: %s
        
        === CÁC MÔN HỌC TRONG APP ===
        %s
        
        === CHỦ ĐỀ THEO TRÌNH ĐỘ ===
        %s
        %s
        
        === CÁCH TRẢ LỜI BẮT BUỘC ===
        %s
        
        Trả lời ngắn gọn, dễ hiểu, phù hợp lứa tuổi %d.
        KHÔNG trả lời câu hỏi không liên quan học tập.
        """.formatted(
                fullname, age, ageGroup, subject, level,
                subjectList, topicList, questionContext,
                hasRelated
                        ? "Đã tìm thấy câu hỏi liên quan trong hệ thống. PHẢI trả lời theo đúng cấu trúc:\n\nĐáp án chính xác là: [đáp án đúng từ hệ thống]\n\nGiải thích: [phân tích tại sao đáp án đó đúng, dễ hiểu theo lứa tuổi]\n\nBạn muốn tìm hiểu thêm điều gì về chủ đề này không?"
                        : "Không có câu hỏi liên quan trong hệ thống. Trả lời tự nhiên, KHÔNG dùng tiêu đề hay nhãn như 'Giải thích:'. Chỉ cần giải thích rõ ràng rồi hỏi thêm học sinh muốn tìm hiểu gì không.",
                age);

        // ✅ THAY ĐỔI: Build messages có history
        List<Map<String, Object>> messages = new ArrayList<>();

        // 1. System prompt luôn đứng đầu
        messages.add(Map.of("role", "system", "content", systemPrompt));

        // 2. Lịch sử hội thoại (user + assistant xen kẽ)
        for (Map<String, String> h : history) {
            messages.add(Map.of("role", h.get("role"), "content", h.get("content")));
        }

        // 3. Tin nhắn hiện tại của user
        messages.add(Map.of("role", "user", "content", userMessage));

        // Giữ nguyên phần gọi HTTP
        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("messages", messages);
        body.put("max_tokens", 1024);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            Map response = restTemplate.postForObject(apiUrl, request, Map.class);
            var choices = (List) response.get("choices");
            var msg = (Map) ((Map) choices.get(0)).get("message");
            return (String) msg.get("content");
        } catch (Exception e) {
            throw new RuntimeException("Groq API lỗi: " + e.getMessage());
        }
    }
}