package com.sc.scifunapi.dto.comment;

import java.util.Date;

public record CommentDto(
        String id,
        String userId,
        String userName,
        String userAvatar,
        String content,
        String parentId,
        int repliesCount,
        Date createdAt,
        Date updatedAt,
        String repliedUserId // dùng để controller gửi notify reply
) {}
