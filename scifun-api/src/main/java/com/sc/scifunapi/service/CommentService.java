// src/main/java/com/sc/scifunapi/service/CommentService.java
package com.sc.scifunapi.service;

import com.sc.scifunapi.entity.Comment;
import com.sc.scifunapi.repository.CommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;

    // Lấy danh sách bình luận gốc (parentId = null) với phân trang
    public Map<String, Object> listRootComments(int page, int limit) {

        // Giống logic Express:
        // page >= 1, limit từ 1–100
        page = Math.max(page, 1);
        limit = Math.min(Math.max(limit, 1), 100);

        Pageable pageable = PageRequest.of(
                page - 1,
                limit,
                Sort.by(Sort.Direction.DESC, "createdAt")  // sort { createdAt: -1 }
        );

        Page<Comment> commentPage = commentRepository.findByParentIdIsNull(pageable);

        long total = commentPage.getTotalElements();
        int totalPages = commentPage.getTotalPages();
        boolean hasMore = (long) page * limit < total;

        Map<String, Object> data = new HashMap<>();
        data.put("items", commentPage.getContent());
        data.put("page", page);
        data.put("limit", limit);
        data.put("total", total);
        data.put("totalPages", totalPages);
        data.put("hasMore", hasMore);

        return data;
    }
}
