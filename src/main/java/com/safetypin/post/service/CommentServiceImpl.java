package com.safetypin.post.service;

import com.safetypin.post.dto.CommentRequest;
import com.safetypin.post.exception.PostNotFoundException;
import com.safetypin.post.model.CommentOnComment;
import com.safetypin.post.model.CommentOnPost;
import com.safetypin.post.model.Post;
import com.safetypin.post.repository.CommentOnCommentRepository;
import com.safetypin.post.repository.CommentOnPostRepository;
import com.safetypin.post.repository.PostRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

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
        // TODO
        return null;
    }

    public void delete(Long commentId, Long userId) {
        // TODO: DEPOL
    }
}
