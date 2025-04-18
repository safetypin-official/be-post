package com.safetypin.post.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.safetypin.post.dto.CommentRequest;
import com.safetypin.post.model.CommentOnComment;
import com.safetypin.post.model.CommentOnPost;

@Service
public interface CommentService {
    CommentOnPost createCommentOnPost(UUID userId, CommentRequest req);

    CommentOnComment createCommentOnComment(UUID userId, CommentRequest req);

    List<?> getAllComments(UUID postId);

    void deleteComment(UUID commentId, UUID userId, boolean isCommentOnComment);
}
