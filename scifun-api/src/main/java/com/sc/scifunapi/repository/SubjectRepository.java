package com.sc.scifunapi.repository;

import com.sc.scifunapi.entity.Subject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface SubjectRepository extends MongoRepository<Subject, String> {
    Page<Subject> findByNameRegex(String regex, Pageable pageable);
    boolean existsByName(String name);
}
