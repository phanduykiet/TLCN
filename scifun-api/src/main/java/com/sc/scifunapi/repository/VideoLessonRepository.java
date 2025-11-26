package com.sc.scifunapi.repository;

import com.sc.scifunapi.entity.VideoLesson;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VideoLessonRepository extends MongoRepository<VideoLesson, String> {

    // Lấy tất cả video theo topicId
    List<VideoLesson> findByTopic_Id(String topicId);

}
