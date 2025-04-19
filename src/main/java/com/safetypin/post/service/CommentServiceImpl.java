package com.safetypin.post.service;

import com.safetypin.post.dto.CommentRequest;
import com.safetypin.post.exception.PostNotFoundException;
import com.safetypin.post.exception.UnauthorizedAccessException;
import com.safetypin.post.model.BasePost;
import com.safetypin.post.model.CommentOnComment;
import com.safetypin.post.model.CommentOnPost;
import com.safetypin.post.model.Post;
import com.safetypin.post.repository.CommentOnCommentRepository;
import com.safetypin.post.repository.CommentOnPostRepository;
import com.safetypin.post.repository.PostRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@AllArgsConstructor
public class CommentServiceImpl implements CommentService {
    private final PostRepository postRepository;
    private final CommentOnPostRepository commentOnPostRepository;
    private final CommentOnCommentRepository commentOnCommentRepository;

    // fetch comment on post
    public Page<CommentOnPost> getCommentOnPost(UUID postId, Pageable pageable) {
        if (postRepository.findById(postId).isEmpty()) {
            throw new IllegalArgumentException("PostId is not found");
        }

        List<CommentOnPost> comments = commentOnPostRepository.findByParentId(postId);

        // sort by timestamp
        List<CommentOnPost> sortedComments = comments.stream()
                .sorted(Comparator.comparing(BasePost::getCreatedAt))
                .toList();

        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), sortedComments.size());

        List<CommentOnPost> pageContent = start >= sortedComments.size() ? Collections.emptyList()
                : sortedComments.subList(start, end);

        return new PageImpl<>(pageContent, pageable, sortedComments.size());
    }

    // fetch comment on comment
    public Page<CommentOnComment> getCommentOnComment(UUID commentId, Pageable pageable) {
        if (commentOnPostRepository.findById(commentId).isEmpty()) {
            throw new IllegalArgumentException("CommentId is not found");
        }

        List<CommentOnComment> comments = commentOnCommentRepository.findByParentId(commentId);

        // sort by timestamp
        List<CommentOnComment> sortedComments = comments.stream()
                .sorted(Comparator.comparing(BasePost::getCreatedAt))
                .toList();

        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), sortedComments.size());

        List<CommentOnComment> pageContent = start >= sortedComments.size() ? Collections.emptyList()
                : sortedComments.subList(start, end);

        return new PageImpl<>(pageContent, pageable, sortedComments.size());
    }

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
