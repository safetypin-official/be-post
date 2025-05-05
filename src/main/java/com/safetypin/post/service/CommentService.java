package com.safetypin.post.service;

import com.safetypin.post.dto.CommentDTO;
import com.safetypin.post.dto.CommentDTOWithPostId;
import com.safetypin.post.dto.CommentRequest;
import com.safetypin.post.model.CommentOnComment;
import com.safetypin.post.model.CommentOnPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public interface CommentService {
    Page<CommentDTOWithPostId> getCommentsByPostedBy(UUID postedBy, Pageable pageable);

    CommentOnPost createCommentOnPost(CommentRequest req);

    CommentOnComment createCommentOnComment(CommentRequest req);

    Page<CommentDTO> getCommentOnPost(UUID postId, Pageable pageable);

    Page<CommentDTO> getCommentOnComment(UUID commentId, Pageable pageable);

    void deleteComment(UUID commentId, UUID userId, boolean isCommentOnComment);
}
