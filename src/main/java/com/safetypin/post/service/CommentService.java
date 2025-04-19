package com.safetypin.post.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.safetypin.post.dto.CommentRequest;
import com.safetypin.post.model.CommentOnComment;
import com.safetypin.post.model.CommentOnPost;

@Service
public interface CommentService {
    CommentOnPost createCommentOnPost(UUID userId, CommentRequest req);

    CommentOnComment createCommentOnComment(UUID userId, CommentRequest req);

    void deleteComment(UUID commentId, UUID userId, boolean isCommentOnComment);
}
