package com.safetypin.post.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.safetypin.post.dto.CommentRequest;
import com.safetypin.post.exception.PostNotFoundException;
import com.safetypin.post.exception.UnauthorizedAccessException;
import com.safetypin.post.model.CommentOnComment;
import com.safetypin.post.model.CommentOnPost;
import com.safetypin.post.model.Post;
import com.safetypin.post.repository.CommentOnCommentRepository;
import com.safetypin.post.repository.CommentOnPostRepository;
import com.safetypin.post.repository.PostRepository;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@AllArgsConstructor
public class CommentServiceImpl implements CommentService {
    private final PostRepository postRepository;
    private final CommentOnPostRepository commentOnPostRepository;
    private final CommentOnCommentRepository commentOnCommentRepository;

    // create comment on post
    public CommentOnPost createCommentOnPost(UUID userId, CommentRequest req) {
        Post post = postRepository.findById(req.getParentId())
                .orElseThrow(() -> new PostNotFoundException("Post not found"));

        CommentOnPost comment = CommentOnPost.builder()
                .parent(post)
                .postedBy(userId)
                .caption(req.getCaption())
                .build();

        return commentOnPostRepository.save(comment);
    }

    // create comment on comment
    public CommentOnComment createCommentOnComment(UUID userId, CommentRequest req) {
        CommentOnPost commentOnPost = commentOnPostRepository.findById(req.getParentId())
                .orElseThrow(() -> new PostNotFoundException("Comment on post not found"));

        CommentOnComment comment = CommentOnComment.builder()
                .parent(commentOnPost)
                .postedBy(userId)
                .caption(req.getCaption())
                .build();

        return commentOnCommentRepository.save(comment);
    }

    public List<?> getAllComments(UUID postId) {
        throw new UnsupportedOperationException("Method not implemented yet");
    }

    @Override
    public void deleteComment(UUID commentId, UUID userId, boolean isCommentOnComment) {
        if (isCommentOnComment) {
            // Delete a child comment (CommentOnComment)
            deleteChildComment(commentId, userId);
        } else {
            // Delete a parent comment (CommentOnPost)
            deleteParentComment(commentId, userId);
        }
    }

    private void deleteParentComment(UUID commentId, UUID userId) {
        // Find the parent comment
        CommentOnPost parentComment = commentOnPostRepository.findById(commentId)
                .orElseThrow(() -> new PostNotFoundException("Comment not found"));

        // Check if the user is authorized to delete this comment
        if (!parentComment.getPostedBy().equals(userId)) {
            throw new UnauthorizedAccessException("User not authorized to delete this comment");
        }

        // When deleting a parent comment, all child comments will be deleted
        // automatically
        // because of the CascadeType.ALL and orphanRemoval=true in the @OneToMany
        // relationship
        commentOnPostRepository.delete(parentComment);
        log.info("Deleted parent comment with ID: {} and all its child comments", commentId);
    }

    private void deleteChildComment(UUID commentId, UUID userId) {
        // Find the child comment
        CommentOnComment childComment = commentOnCommentRepository.findById(commentId)
                .orElseThrow(() -> new PostNotFoundException("Comment not found"));

        // Check if the user is authorized to delete this comment
        if (!childComment.getPostedBy().equals(userId)) {
            throw new UnauthorizedAccessException("User not authorized to delete this comment");
        }

        // Delete the child comment
        commentOnCommentRepository.delete(childComment);
        log.info("Deleted child comment with ID: {}", commentId);
    }
}
