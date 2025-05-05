package com.safetypin.post.service;

import com.safetypin.post.dto.CommentDTO;
import com.safetypin.post.dto.CommentRequest;
import com.safetypin.post.dto.UserDetails;
import com.safetypin.post.exception.PostNotFoundException;
import com.safetypin.post.exception.UnauthorizedAccessException;
import com.safetypin.post.model.CommentOnComment;
import com.safetypin.post.model.CommentOnPost;
import com.safetypin.post.model.Post;
import com.safetypin.post.model.Role;
import com.safetypin.post.repository.CommentOnCommentRepository;
import com.safetypin.post.repository.CommentOnPostRepository;
import com.safetypin.post.repository.PostRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CommentServiceImplTest {

    private PostRepository postRepository;
    private PostService postService;
    private CommentOnPostRepository commentOnPostRepository;
    private CommentOnCommentRepository commentOnCommentRepository;
    private CommentServiceImpl commentService;

    @BeforeEach
    void setUp() {
        postRepository = mock(PostRepository.class);
        postService = mock(PostService.class);
        commentOnPostRepository = mock(CommentOnPostRepository.class);
        commentOnCommentRepository = mock(CommentOnCommentRepository.class);
        commentService = new CommentServiceImpl(postRepository, postService, commentOnPostRepository, commentOnCommentRepository);

        // Mock SecurityContextHolder
        SecurityContext securityContext = mock(SecurityContext.class);
        Authentication authentication = mock(Authentication.class);
        UserDetails userDetails = new UserDetails(Role.REGISTERED_USER, true, UUID.randomUUID(), "Test User");

        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
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

        CommentOnPost savedComment = commentService.createCommentOnPost(req);

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

        assertThrows(PostNotFoundException.class, () -> commentService.createCommentOnPost(req));
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

        CommentOnComment savedReply = commentService.createCommentOnComment(req);

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

        assertThrows(PostNotFoundException.class, () -> commentService.createCommentOnComment(req));
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

    @Test
    void getCommentOnPost_shouldReturnComments_whenPostExists() {
        UUID postId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 10);

        Post post = new Post.Builder()
                .id(postId)
                .title("Test")
                .caption("Caption")
                .location(10.0, 20.0)
                .build();

        CommentOnPost comment1 = CommentOnPost.builder()
                .id(UUID.randomUUID())
                .caption("First Comment")
                .createdAt(LocalDateTime.now().minusMinutes(10))
                .build();

        CommentOnPost comment2 = CommentOnPost.builder()
                .id(UUID.randomUUID())
                .caption("Second Comment")
                .createdAt(LocalDateTime.now())
                .build();

        when(postRepository.findById(postId)).thenReturn(Optional.of(post));
        when(commentOnPostRepository.findByParentId(postId)).thenReturn(List.of(comment1, comment2));

        Page<CommentDTO> result = commentService.getCommentOnPost(postId, pageable);

        assertEquals(2, result.getContent().size());
        assertEquals("Second Comment", result.getContent().getFirst().getCaption());
    }

    @Test
    void getCommentOnPost_shouldThrowException_whenPostDoesNotExist() {
        UUID postId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 10);

        when(postRepository.findById(postId)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                commentService.getCommentOnPost(postId, pageable));

        assertEquals("PostId is not found", exception.getMessage());
    }

    @Test
    void getCommentOnComment_shouldReturnComments_whenCommentExists() {
        UUID commentId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 10);

        CommentOnPost parentComment = CommentOnPost.builder()
                .id(commentId)
                .caption("Parent Comment")
                .createdAt(LocalDateTime.now().minusHours(1))
                .build();

        CommentOnComment reply1 = CommentOnComment.builder()
                .id(UUID.randomUUID())
                .caption("Reply 1")
                .createdAt(LocalDateTime.now().minusMinutes(30))
                .build();

        CommentOnComment reply2 = CommentOnComment.builder()
                .id(UUID.randomUUID())
                .caption("Reply 2")
                .createdAt(LocalDateTime.now().minusMinutes(10))
                .build();

        when(commentOnPostRepository.findById(commentId)).thenReturn(Optional.of(parentComment));
        when(commentOnCommentRepository.findByParentId(commentId)).thenReturn(List.of(reply1, reply2));

        Page<CommentDTO> result = commentService.getCommentOnComment(commentId, pageable);

        assertEquals(2, result.getContent().size());
        assertEquals("Reply 1", result.getContent().getFirst().getCaption());
    }

    @Test
    void getCommentOnComment_shouldThrowException_whenCommentDoesNotExist() {
        UUID commentId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 10);

        when(commentOnPostRepository.findById(commentId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () ->
                commentService.getCommentOnComment(commentId, pageable));
    }

    // tests for when page offset is greater than comment counts
    @Test
    void getCommentOnPost_shouldReturnEmptyPage_whenOffsetIsGreaterThanCommentCount() {
        UUID postId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(10, 10); // Offset is greater than comment count

        Post post = new Post.Builder()
                .id(postId)
                .title("Test")
                .caption("Caption")
                .location(10.0, 20.0)
                .build();

        when(postRepository.findById(postId)).thenReturn(Optional.of(post));
        when(commentOnPostRepository.findByParentId(postId)).thenReturn(List.of());

        Page<CommentDTO> result = commentService.getCommentOnPost(postId, pageable);

        assertEquals(0, result.getContent().size());
    }

    @Test
    void getCommentOnComment_shouldReturnEmptyPage_whenOffsetIsGreaterThanCommentCount() {
        UUID commentId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(10, 10); // Offset is greater than comment count

        CommentOnPost parentComment = CommentOnPost.builder()
                .id(commentId)
                .caption("Parent Comment")
                .createdAt(LocalDateTime.now().minusHours(1))
                .build();

        when(commentOnPostRepository.findById(commentId)).thenReturn(Optional.of(parentComment));
        when(commentOnCommentRepository.findByParentId(commentId)).thenReturn(List.of());

        Page<CommentDTO> result = commentService.getCommentOnComment(commentId, pageable);

        assertEquals(0, result.getContent().size());
    }

    @Test
    void testCreateCommentOnPost_NoSecurityContext() {
        // Clear security context
        SecurityContextHolder.clearContext();

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

        // Should throw IllegalStateException when security context is not available
        assertThrows(NullPointerException.class, () -> commentService.createCommentOnPost(req));
        verify(commentOnPostRepository, never()).save(any());
    }

    @Test
    void testCreateCommentOnComment_NoSecurityContext() {
        // Clear security context
        SecurityContextHolder.clearContext();

        UUID commentId = UUID.randomUUID();
        CommentRequest req = new CommentRequest();
        req.setCaption("Nice comment!");
        req.setParentId(commentId);

        CommentOnPost parentComment = CommentOnPost.builder()
                .id(commentId)
                .caption("Parent comment")
                .build();

        when(commentOnPostRepository.findById(commentId)).thenReturn(Optional.of(parentComment));

        assertThrows(NullPointerException.class, () -> commentService.createCommentOnComment(req));
        verify(commentOnCommentRepository, never()).save(any());
    }

}
