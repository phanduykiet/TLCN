// src/main/java/com/sc/scifunapi/repository/NotificationRepository.java
package com.sc.scifunapi.repository;

import com.sc.scifunapi.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepository extends MongoRepository<Notification, String> {

    // Lấy danh sách notification theo user với phân trang
    Page<Notification> findByUserId(String userId, Pageable pageable);

    // Tổng số notification của user
    long countByUserId(String userId);

    // Tổng số notification chưa đọc
    long countByUserIdAndIsReadFalse(String userId);

    Optional<Notification> findByIdAndUserId(String id, String userId);

    // Lấy tất cả thông báo chưa đọc của user
    List<Notification> findByUserIdAndIsReadFalse(String userId);
}
