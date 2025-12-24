package com.sc.scifunapi.dto.comment;

import com.sc.scifunapi.entity.Comment;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class PagedCommentsResponse {
    private String parentId;     // null với root comments
    private List<Comment> items;
    private int page;
    private int limit;
    private long total;
    private int totalPages;
    private boolean hasMore;
}
