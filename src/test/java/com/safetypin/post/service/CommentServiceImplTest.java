package com.safetypin.post.service;

import com.safetypin.post.dto.CommentRequest;
import com.safetypin.post.exception.PostNotFoundException;
import com.safetypin.post.exception.UnauthorizedAccessException;
import com.safetypin.post.model.CommentOnComment;
import com.safetypin.post.model.CommentOnPost;
import com.safetypin.post.model.Post;
import com.safetypin.post.repository.CommentOnCommentRepository;
import com.safetypin.post.repository.CommentOnPostRepository;
import com.safetypin.post.repository.PostRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CommentServiceImplTest {

    private PostRepository postRepository;
    private CommentOnPostRepository commentOnPostRepository;
    private CommentOnCommentRepository commentOnCommentRepository;
    private CommentServiceImpl commentService;

    @BeforeEach
    void setUp() {
        postRepository = mock(PostRepository.class);
        commentOnPostRepository = mock(CommentOnPostRepository.class);
        commentOnCommentRepository = mock(CommentOnCommentRepository.class);
        commentService = new CommentServiceImpl(postRepository, commentOnPostRepository, commentOnCommentRepository);
    }

    @Test
    void testCreateCommentOnPost_Success() {
        UUID userId = UUID.randomUUID();
        UUID postId = UUID.randomUUID();
        CommentRequest req = new CommentRequest();
        req.setCaption("Nice post!");
        req.setParentId(postId);

        Post mockPost = Post.builder()
                .id(postId)
                .caption("Original Post")
                .title("Post Title")
                .category("General")
                .createdAt(LocalDateTime.now())
                .latitude(0.0)
                .longitude(0.0)
                .build();

        when(postRepository.findById(postId)).thenReturn(Optional.of(mockPost));

        CommentOnPost mockComment = CommentOnPost.builder()
                .parent(mockPost)
                .postedBy(userId)
                .caption(req.getCaption())
                .build();

        when(commentOnPostRepository.save(any(CommentOnPost.class))).thenReturn(mockComment);

        CommentOnPost savedComment = commentService.createCommentOnPost(userId, req);

        assertNotNull(savedComment);
        assertEquals(req.getCaption(), savedComment.getCaption());
        assertEquals(userId, savedComment.getPostedBy());
        verify(commentOnPostRepository).save(any(CommentOnPost.class));
    }

    @Test
    void testCreateCommentOnPost_PostNotFound() {
        UUID userId = UUID.randomUUID();
        UUID postId = UUID.randomUUID();
        CommentRequest req = new CommentRequest();
        req.setCaption("Nice post!");
        req.setParentId(postId);

        when(postRepository.findById(postId)).thenReturn(Optional.empty());

        assertThrows(PostNotFoundException.class, () -> commentService.createCommentOnPost(userId, req));
        verify(commentOnPostRepository, never()).save(any());
    }

    @Test
    void testCreateCommentOnComment_Success() {
        UUID userId = UUID.randomUUID();
        UUID commentId = UUID.randomUUID();
        CommentRequest req = new CommentRequest();
        req.setCaption("Nice post!");
        req.setParentId(commentId);

        CommentOnPost parentComment = CommentOnPost.builder()
                .id(commentId)
                .caption("Top level comment")
                .createdAt(LocalDateTime.now())
                .build();

        when(commentOnPostRepository.findById(commentId)).thenReturn(Optional.of(parentComment));

        CommentOnComment expectedReply = CommentOnComment.builder()
                .parent(parentComment)
                .postedBy(userId)
                .caption(req.getCaption())
                .build();

        when(commentOnCommentRepository.save(any(CommentOnComment.class))).thenReturn(expectedReply);

        CommentOnComment savedReply = commentService.createCommentOnComment(userId, req);

        assertNotNull(savedReply);
        assertEquals(req.getCaption(), savedReply.getCaption());
        assertEquals(userId, savedReply.getPostedBy());
        assertEquals(parentComment, savedReply.getParent());
        verify(commentOnCommentRepository).save(any(CommentOnComment.class));
    }

    @Test
    void testCreateCommentOnComment_CommentNotFound() {
        UUID userId = UUID.randomUUID();
        UUID commentId = UUID.randomUUID();
        CommentRequest req = new CommentRequest();
        req.setCaption("I agree!");
        req.setParentId(commentId);

        when(commentOnPostRepository.findById(commentId)).thenReturn(Optional.empty());

        assertThrows(PostNotFoundException.class, () -> commentService.createCommentOnComment(userId, req));
        verify(commentOnCommentRepository, never()).save(any());
    }

    @Test
    void testDeleteParentComment_Success() {
        // Prepare test data
        UUID commentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        CommentOnPost comment = CommentOnPost.builder()
                .id(commentId)
                .caption("Parent comment")
                .postedBy(userId) // Same user is deleting
                .createdAt(LocalDateTime.now())
                .build();

        // Mock repository response
        when(commentOnPostRepository.findById(commentId)).thenReturn(Optional.of(comment));

        // Execute
        commentService.deleteComment(commentId, userId, false);

        // Verify
        verify(commentOnPostRepository).findById(commentId);
        verify(commentOnPostRepository).delete(comment);
    }

    @Test
    void testDeleteParentComment_NotFound() {
        // Prepare test data
        UUID commentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        // Mock repository response
        when(commentOnPostRepository.findById(commentId)).thenReturn(Optional.empty());

        // Execute and verify
        assertThrows(PostNotFoundException.class,
                () -> commentService.deleteComment(commentId, userId, false));

        verify(commentOnPostRepository).findById(commentId);
        verify(commentOnPostRepository, never()).delete(any());
    }

    @Test
    void testDeleteParentComment_Unauthorized() {
        // Prepare test data
        UUID commentId = UUID.randomUUID();
        UUID commentOwnerId = UUID.randomUUID();
        UUID differentUserId = UUID.randomUUID(); // Different user

        CommentOnPost comment = CommentOnPost.builder()
                .id(commentId)
                .caption("Parent comment")
                .postedBy(commentOwnerId) // Different from the user trying to delete
                .createdAt(LocalDateTime.now())
                .build();

        // Mock repository response
        when(commentOnPostRepository.findById(commentId)).thenReturn(Optional.of(comment));

        // Execute and verify
        assertThrows(UnauthorizedAccessException.class,
                () -> commentService.deleteComment(commentId, differentUserId, false));

        verify(commentOnPostRepository).findById(commentId);
        verify(commentOnPostRepository, never()).delete(any());
    }

    @Test
    void testDeleteChildComment_Success() {
        // Prepare test data
        UUID commentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        CommentOnComment comment = CommentOnComment.builder()
                .id(commentId)
                .caption("Reply comment")
                .postedBy(userId) // Same user is deleting
                .createdAt(LocalDateTime.now())
                .build();

        // Mock repository response
        when(commentOnCommentRepository.findById(commentId)).thenReturn(Optional.of(comment));

        // Execute
        commentService.deleteComment(commentId, userId, true);

        // Verify
        verify(commentOnCommentRepository).findById(commentId);
        verify(commentOnCommentRepository).delete(comment);
    }

    @Test
    void testDeleteChildComment_NotFound() {
        // Prepare test data
        UUID commentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        // Mock repository response
        when(commentOnCommentRepository.findById(commentId)).thenReturn(Optional.empty());

        // Execute and verify
        assertThrows(PostNotFoundException.class,
                () -> commentService.deleteComment(commentId, userId, true));

        verify(commentOnCommentRepository).findById(commentId);
        verify(commentOnCommentRepository, never()).delete(any());
    }

    @Test
    void testDeleteChildComment_Unauthorized() {
        // Prepare test data
        UUID commentId = UUID.randomUUID();
        UUID commentOwnerId = UUID.randomUUID();
        UUID differentUserId = UUID.randomUUID(); // Different user

        CommentOnComment comment = CommentOnComment.builder()
                .id(commentId)
                .caption("Reply comment")
                .postedBy(commentOwnerId) // Different from the user trying to delete
                .createdAt(LocalDateTime.now())
                .build();

        // Mock repository response
        when(commentOnCommentRepository.findById(commentId)).thenReturn(Optional.of(comment));

        // Execute and verify
        assertThrows(UnauthorizedAccessException.class,
                () -> commentService.deleteComment(commentId, differentUserId, true));

        verify(commentOnCommentRepository).findById(commentId);
        verify(commentOnCommentRepository, never()).delete(any());
    }
}
