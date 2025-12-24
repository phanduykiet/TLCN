// src/main/java/com/sc/scifunapi/repository/CommentRepository.java
package com.sc.scifunapi.repository;

import com.sc.scifunapi.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface CommentRepository extends MongoRepository<Comment, String> {

    Page<Comment> findByParentIdIsNull(Pageable pageable);

    Page<Comment> findByParentId(String parentId, Pageable pageable);

    Optional<Comment> findById(String id);
}
