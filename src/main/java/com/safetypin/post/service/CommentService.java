package com.safetypin.post.service;

import com.safetypin.post.dto.CommentRequest;
import com.safetypin.post.model.CommentOnComment;
import com.safetypin.post.model.CommentOnPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public interface CommentService {
    CommentOnPost createCommentOnPost(UUID userId, CommentRequest req);

    CommentOnComment createCommentOnComment(UUID userId, CommentRequest req);

    List<?> getAllComments(UUID postId);

    Page<CommentOnPost> getCommentOnPost(UUID postId, Pageable pageable);

    Page<CommentOnComment> getCommentOnComment(UUID commentId, Pageable pageable);

    void deleteComment(UUID commentId, UUID userId, boolean isCommentOnComment);
}
