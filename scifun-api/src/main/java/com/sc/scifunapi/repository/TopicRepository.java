package com.sc.scifunapi.repository;

import com.sc.scifunapi.entity.Topic;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TopicRepository extends MongoRepository<Topic, String> {
    List<Topic> findBySubject_Id(String subjectId);
}
