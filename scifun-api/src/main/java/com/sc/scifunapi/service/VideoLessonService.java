package com.sc.scifunapi.service;

import com.sc.scifunapi.entity.Topic;
import com.sc.scifunapi.entity.VideoLesson;
import com.sc.scifunapi.repository.TopicRepository;
import com.sc.scifunapi.repository.VideoLessonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class VideoLessonService {

    private final VideoLessonRepository videoLessonRepository;
    private final TopicRepository topicRepository;

    // Thêm video lesson
    public Map<String, Object> createVideoLessonSv(Map<String, Object> data) {

        String title = data.get("title") != null ? data.get("title").toString() : null;
        String url = data.get("url") != null ? data.get("url").toString() : null;
        String topicId = data.get("topic") != null ? data.get("topic").toString() : null;

        Integer duration = null;
        if (data.get("duration") != null) {
            duration = Integer.parseInt(data.get("duration").toString());
        }

        if (title == null || title.isBlank()) {
            throw new RuntimeException("Tiêu đề video không được để trống");
        }
        if (url == null || url.isBlank()) {
            throw new RuntimeException("URL video không được để trống");
        }
        if (topicId == null || topicId.isBlank()) {
            throw new RuntimeException("Topic ID không được để trống");
        }

        // Convert URL sang embed format nếu là YouTube
        String embedUrl = convertToYoutubeEmbed(url);

        // Tìm topic
        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new RuntimeException("Topic không tồn tại"));

        VideoLesson lesson = VideoLesson.builder()
                .title(title)
                .url(embedUrl)
                .duration(duration)
                .topic(topic)
                .build();

        VideoLesson saved = videoLessonRepository.save(lesson);

        // Trả về giống populate("topic", "-__v")
        Map<String, Object> topicMap = new HashMap<>();
        topicMap.put("_id", topic.getId());
        topicMap.put("name", topic.getName());
        topicMap.put("description", topic.getDescription());

        Map<String, Object> res = new HashMap<>();
        res.put("_id", saved.getId());
        res.put("title", saved.getTitle());
        res.put("url", saved.getUrl());
        res.put("duration", saved.getDuration());
        res.put("topic", topicMap);

        return res;
    }

    /**
     * Convert YouTube URL sang embed format
     * - youtu.be/VIDEO_ID
     * - youtube.com/watch?v=VIDEO_ID
     * - youtube.com/embed/VIDEO_ID
     */
    private String convertToYoutubeEmbed(String url) {
        if (url == null || url.isBlank()) return url;

        String[] patterns = new String[]{
                "(?:https?://)?(?:www\\.)?youtu\\.be/([a-zA-Z0-9_-]{11})",
                "(?:https?://)?(?:www\\.)?youtube\\.com/watch\\?v=([a-zA-Z0-9_-]{11})",
                "(?:https?://)?(?:www\\.)?youtube\\.com/embed/([a-zA-Z0-9_-]{11})"
        };

        for (String p : patterns) {
            Pattern pattern = Pattern.compile(p);
            Matcher matcher = pattern.matcher(url);
            if (matcher.find()) {
                String videoId = matcher.group(1);
                return "https://www.youtube.com/embed/" + videoId;
            }
        }

        // Không phải link YouTube → trả lại URL gốc
        return url;
    }

    // Sửa video lesson
    public Map<String, Object> updateVideoLessonSv(String id, Map<String, Object> updateData) {
        if (id == null || id.isBlank()) {
            throw new RuntimeException("ID video lesson không hợp lệ");
        }

        VideoLesson lesson = videoLessonRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Video lesson không tồn tại"));

        // Update title
        if (updateData.get("title") != null) {
            String title = updateData.get("title").toString();
            if (!title.isBlank()) {
                lesson.setTitle(title);
            }
        }

        // Update URL (convert sang embed nếu là YouTube)
        if (updateData.get("url") != null) {
            String url = updateData.get("url").toString();
            if (!url.isBlank()) {
                String embedUrl = convertToYoutubeEmbed(url);
                lesson.setUrl(embedUrl);
            }
        }

        // Update duration
        if (updateData.get("duration") != null) {
            lesson.setDuration(Integer.parseInt(updateData.get("duration").toString()));
        }

        // Update topic nếu truyền topic mới
        if (updateData.get("topic") != null) {
            String topicId = updateData.get("topic").toString();
            Topic topic = topicRepository.findById(topicId)
                    .orElseThrow(() -> new RuntimeException("Topic không tồn tại"));
            lesson.setTopic(topic);
        }

        VideoLesson saved = videoLessonRepository.save(lesson);

        return buildResponse(saved);
    }

    // Xóa video lesson
    public Map<String, Object> deleteVideoLessonSv(String id) {
        if (id == null || id.isBlank()) {
            throw new RuntimeException("ID video lesson không hợp lệ");
        }

        VideoLesson lesson = videoLessonRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Video lesson không tồn tại"));

        videoLessonRepository.delete(lesson);

        Map<String, Object> res = new HashMap<>();
        res.put("message", "Xóa thành công");
        res.put("videoLesson", buildResponse(lesson)); // trả lại thông tin giống Express
        return res;
    }

    // Build response giống populate("topic", "-__v")
    private Map<String, Object> buildResponse(VideoLesson saved) {
        Topic topic = saved.getTopic();

        Map<String, Object> topicMap = null;
        if (topic != null) {
            topicMap = new HashMap<>();
            topicMap.put("_id", topic.getId());
            topicMap.put("name", topic.getName());
            topicMap.put("description", topic.getDescription());
        }

        Map<String, Object> res = new HashMap<>();
        res.put("_id", saved.getId());
        res.put("title", saved.getTitle());
        res.put("url", saved.getUrl());
        res.put("duration", saved.getDuration());
        res.put("topic", topicMap);

        return res;
    }

    public Map<String, Object> getVideoLessonsSv(int page, int limit, String topicId) {

        if (page <= 0) page = 1;
        if (limit <= 0) limit = 10;

        // Lọc theo topicId nếu có, giống filter.topic = topicId
        List<VideoLesson> all;
        if (topicId != null && !topicId.isBlank()) {
            all = videoLessonRepository.findByTopic_Id(topicId);
        } else {
            all = videoLessonRepository.findAll();
        }

        // Sort giống sort({ createdAt: -1 }) — ở đây sort tạm theo id giảm dần
        all.sort(Comparator.comparing(VideoLesson::getId).reversed());

        int total = all.size();
        int fromIndex = Math.min((page - 1) * limit, total);
        int toIndex = Math.min(fromIndex + limit, total);

        List<Map<String, Object>> data = all.subList(fromIndex, toIndex)
                .stream()
                .map(this::buildResponse) // dùng lại format trả về như các API khác
                .toList();

        Map<String, Object> res = new HashMap<>();
        res.put("page", page);
        res.put("limit", limit);
        res.put("total", total);
        res.put("totalPages", (int) Math.ceil(total / (double) limit));
        res.put("data", data);

        return res;
    }

    // Lấy chi tiết video lesson
    public Map<String, Object> getVideoLessonByIdSv(String id) {

        if (id == null || id.isBlank()) {
            throw new RuntimeException("ID video lesson không hợp lệ");
        }

        VideoLesson videoLesson = videoLessonRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Video lesson không tồn tại"));

        // dùng lại hàm buildResponse để format giống các API khác
        return buildResponse(videoLesson);
    }
}
