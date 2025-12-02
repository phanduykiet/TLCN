// src/main/java/com/sc/scifunapi/repository/CommentRepository.java
package com.sc.scifunapi.repository;

import com.sc.scifunapi.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CommentRepository extends MongoRepository<Comment, String> {

    // comment gốc (parentId = null)
    Page<Comment> findByParentIdIsNull(Pageable pageable);

    // comment con theo parent
    Page<Comment> findByParentId(String parentId, Pageable pageable);
}
