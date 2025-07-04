package com.safetypin.post.controller;

import com.safetypin.post.dto.*;
import com.safetypin.post.exception.InvalidPostDataException;
import com.safetypin.post.exception.PostNotFoundException;
import com.safetypin.post.exception.UnauthorizedAccessException;
import com.safetypin.post.model.CommentOnComment;
import com.safetypin.post.model.CommentOnPost;
import com.safetypin.post.model.Post;
import com.safetypin.post.model.Role;
import com.safetypin.post.service.CommentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentControllerTest {

    @Mock
    private CommentService commentService;

    @Mock
    private Authentication authentication;

    @Mock
    private SecurityContext securityContext;

    @InjectMocks
    private CommentController commentController;

    private UUID userId;
    private CommentRequest validCommentRequest;
    private UserDetails userDetails;
    private CommentOnPost commentOnPost;
    private CommentOnComment commentOnComment;
    private CommentDTO commentOnPostDTO;
    private CommentDTO commentOnCommentDTO;
    private PostedByData postedByData;
    private UUID postId;
    private UUID commentId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        postId = UUID.randomUUID();
        commentId = UUID.randomUUID();
        userDetails = new UserDetails(Role.REGISTERED_USER, true, userId, "Test User");

        // Setup comment request
        UUID parentId = UUID.randomUUID();
        validCommentRequest = new CommentRequest("This is a test comment", parentId);

        // Setup post with coordinates
        Post parentPost = Post.builder()
                .id(parentId)
                .title("Test Post")
                .caption("Test Content")
                .createdAt(LocalDateTime.now())
                .latitude(10.0) // Valid latitude
                .longitude(20.0) // Valid longitude
                .build();

        commentOnPost = CommentOnPost.builder()
                .id(UUID.randomUUID())
                .parent(parentPost)
                .caption("Test comment on post")
                .postedBy(userId)
                .createdAt(LocalDateTime.now())
                .build();


        commentOnComment = CommentOnComment.builder()
                .id(UUID.randomUUID())
                .parent(commentOnPost)
                .caption("Test reply")
                .postedBy(userId)
                .createdAt(LocalDateTime.now())
                .build();

        postedByData = new PostedByData(userId, "Kimi", "Kimi.jpg");


        commentOnPostDTO = new CommentDTO(commentOnPost, postedByData);
    }

    // Helper method to mock security context
    private void mockSecurityContext() {
        lenient().when(authentication.getPrincipal()).thenReturn(userDetails);
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    // Helper method to validate response with null checks
    private void assertResponseNotNull(ResponseEntity<PostResponse> response) {
        assertNotNull(response);
        assertNotNull(response.getBody());
    }

    // -------------------- createCommentOnPost Tests --------------------

    @Test
    void createCommentOnPost_Success() {
        // Setup
        mockSecurityContext();
        when(commentService.createCommentOnPost(any(CommentRequest.class))).thenReturn(commentOnPost);

        // Execute
        ResponseEntity<PostResponse> response = commentController.createCommentOnPost(validCommentRequest);

        // Verify
        assertResponseNotNull(response);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertTrue(Objects.requireNonNull(response.getBody()).isSuccess());
        assertEquals("Comment created successfully", response.getBody().getMessage());
        assertEquals(commentOnPost, response.getBody().getData());
        verify(commentService).createCommentOnPost(validCommentRequest);
    }

    @Test
    void createCommentOnPost_PostNotFound() {
        // Setup
        mockSecurityContext();
        when(commentService.createCommentOnPost(any(CommentRequest.class)))
                .thenThrow(new PostNotFoundException("Post not found"));

        // Execute
        ResponseEntity<PostResponse> response = commentController.createCommentOnPost(validCommentRequest);

        // Verify
        assertResponseNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertFalse(Objects.requireNonNull(response.getBody()).isSuccess());
        assertEquals("Post not found", response.getBody().getMessage());
        assertNull(response.getBody().getData());
        verify(commentService).createCommentOnPost(validCommentRequest);
    }

    @Test
    void createCommentOnPost_InvalidData() {
        // Setup
        mockSecurityContext();
        when(commentService.createCommentOnPost(any(CommentRequest.class)))
                .thenThrow(new InvalidPostDataException("Invalid comment data"));

        // Execute
        ResponseEntity<PostResponse> response = commentController.createCommentOnPost(validCommentRequest);

        // Verify
        assertResponseNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertFalse(Objects.requireNonNull(response.getBody()).isSuccess());
        assertEquals("Invalid comment data", response.getBody().getMessage());
        assertNull(response.getBody().getData());
        verify(commentService).createCommentOnPost(validCommentRequest);
    }

    @Test
    void createCommentOnPost_UnexpectedException() {
        // Setup
        mockSecurityContext();
        when(commentService.createCommentOnPost(any(CommentRequest.class)))
                .thenThrow(new RuntimeException("Unexpected error"));

        // Execute
        ResponseEntity<PostResponse> response = commentController.createCommentOnPost(validCommentRequest);

        // Verify
        assertResponseNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertFalse(Objects.requireNonNull(response.getBody()).isSuccess());
        assertTrue(response.getBody().getMessage().contains("Unexpected error"));
        assertNull(response.getBody().getData());
        verify(commentService).createCommentOnPost(validCommentRequest);
    }

    // -------------------- createCommentOnComment Tests --------------------

    @Test
    void createCommentOnComment_Success() {
        // Setup
        mockSecurityContext();
        when(commentService.createCommentOnComment(any(CommentRequest.class))).thenReturn(commentOnComment);

        // Execute
        ResponseEntity<PostResponse> response = commentController.createCommentOnComment(validCommentRequest);

        // Verify
        assertResponseNotNull(response);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertTrue(Objects.requireNonNull(response.getBody()).isSuccess());
        assertEquals("Comment created successfully", response.getBody().getMessage());
        assertEquals(commentOnComment, response.getBody().getData());
        verify(commentService).createCommentOnComment(validCommentRequest);
    }

    @Test
    void createCommentOnComment_CommentNotFound() {
        // Setup
        mockSecurityContext();
        when(commentService.createCommentOnComment(any(CommentRequest.class)))
                .thenThrow(new PostNotFoundException("Parent comment not found"));

        // Execute
        ResponseEntity<PostResponse> response = commentController.createCommentOnComment(validCommentRequest);

        // Verify
        assertResponseNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertFalse(Objects.requireNonNull(response.getBody()).isSuccess());
        assertEquals("Parent comment not found", response.getBody().getMessage());
        assertNull(response.getBody().getData());
        verify(commentService).createCommentOnComment(validCommentRequest);
    }

    @Test
    void createCommentOnComment_InvalidData() {
        // Setup
        mockSecurityContext();
        when(commentService.createCommentOnComment(any(CommentRequest.class)))
                .thenThrow(new InvalidPostDataException("Invalid reply data"));

        // Execute
        ResponseEntity<PostResponse> response = commentController.createCommentOnComment(validCommentRequest);

        // Verify
        assertResponseNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertFalse(Objects.requireNonNull(response.getBody()).isSuccess());
        assertEquals("Invalid reply data", response.getBody().getMessage());
        assertNull(response.getBody().getData());
        verify(commentService).createCommentOnComment(validCommentRequest);
    }

    @Test
    void createCommentOnComment_UnexpectedException() {
        // Setup
        mockSecurityContext();
        when(commentService.createCommentOnComment(any(CommentRequest.class)))
                .thenThrow(new RuntimeException("Database connection failed"));

        // Execute
        ResponseEntity<PostResponse> response = commentController.createCommentOnComment(validCommentRequest);

        // Verify
        assertResponseNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertFalse(Objects.requireNonNull(response.getBody()).isSuccess());
        assertTrue(response.getBody().getMessage().contains("Database connection failed"));
        assertNull(response.getBody().getData());
        verify(commentService).createCommentOnComment(validCommentRequest);
    }

    // -------------------- deleteCommentOnPost Tests --------------------

    @Test
    void deleteCommentOnPost_Success() {
        // Setup
        mockSecurityContext();
        doNothing().when(commentService).deleteComment(commentId, userId, false);

        // Execute
        ResponseEntity<PostResponse> response = commentController.deleteCommentOnPost(commentId);

        // Verify
        assertResponseNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(Objects.requireNonNull(response.getBody()).isSuccess());
        assertEquals("Comment and all its replies deleted successfully", response.getBody().getMessage());

        // Verify response data contains expected properties
        @SuppressWarnings("unchecked")
        Map<String, Object> responseData = (Map<String, Object>) response.getBody().getData();
        assertNotNull(responseData);
        assertEquals(commentId, responseData.get("commentId"));
        assertEquals(userId, responseData.get("deletedBy"));
        assertEquals("parent comment", responseData.get("type"));

        verify(commentService).deleteComment(commentId, userId, false);
    }

    @Test
    void deleteCommentOnPost_CommentNotFound() {
        // Setup
        mockSecurityContext();
        doThrow(new PostNotFoundException("Comment not found")).when(commentService).deleteComment(commentId, userId,
                false);

        // Execute
        ResponseEntity<PostResponse> response = commentController.deleteCommentOnPost(commentId);

        // Verify
        assertResponseNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertFalse(Objects.requireNonNull(response.getBody()).isSuccess());
        assertEquals("Comment not found", response.getBody().getMessage());
        assertNull(response.getBody().getData());
        verify(commentService).deleteComment(commentId, userId, false);
    }

    @Test
    void deleteCommentOnPost_Unauthorized() {
        // Setup
        mockSecurityContext();
        doThrow(new UnauthorizedAccessException("User not authorized to delete this comment"))
                .when(commentService).deleteComment(commentId, userId, false);

        // Execute
        ResponseEntity<PostResponse> response = commentController.deleteCommentOnPost(commentId);

        // Verify
        assertResponseNotNull(response);
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertFalse(Objects.requireNonNull(response.getBody()).isSuccess());
        assertEquals("User not authorized to delete this comment", response.getBody().getMessage());
        assertNull(response.getBody().getData());
        verify(commentService).deleteComment(commentId, userId, false);
    }

    @Test
    void deleteCommentOnPost_UnexpectedException() {
        // Setup
        mockSecurityContext();
        doThrow(new RuntimeException("Database error")).when(commentService).deleteComment(commentId, userId, false);

        // Execute
        ResponseEntity<PostResponse> response = commentController.deleteCommentOnPost(commentId);

        // Verify
        assertResponseNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertFalse(Objects.requireNonNull(response.getBody()).isSuccess());
        assertTrue(response.getBody().getMessage().contains("Database error"));
        assertNull(response.getBody().getData());
        verify(commentService).deleteComment(commentId, userId, false);
    }

    // -------------------- deleteCommentOnComment Tests --------------------

    @Test
    void deleteCommentOnComment_Success() {
        // Setup
        mockSecurityContext();
        doNothing().when(commentService).deleteComment(commentId, userId, true);

        // Execute
        ResponseEntity<PostResponse> response = commentController.deleteCommentOnComment(commentId);

        // Verify
        assertResponseNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(Objects.requireNonNull(response.getBody()).isSuccess());
        assertEquals("Reply deleted successfully", response.getBody().getMessage());

        // Verify response data contains expected properties
        @SuppressWarnings("unchecked")
        Map<String, Object> responseData = (Map<String, Object>) response.getBody().getData();
        assertNotNull(responseData);
        assertEquals(commentId, responseData.get("commentId"));
        assertEquals(userId, responseData.get("deletedBy"));
        assertEquals("child comment", responseData.get("type"));

        verify(commentService).deleteComment(commentId, userId, true);
    }

    @Test
    void deleteCommentOnComment_CommentNotFound() {
        // Setup
        mockSecurityContext();
        doThrow(new PostNotFoundException("Reply not found")).when(commentService).deleteComment(commentId, userId,
                true);

        // Execute
        ResponseEntity<PostResponse> response = commentController.deleteCommentOnComment(commentId);

        // Verify
        assertResponseNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertFalse(Objects.requireNonNull(response.getBody()).isSuccess());
        assertEquals("Reply not found", response.getBody().getMessage());
        assertNull(response.getBody().getData());
        verify(commentService).deleteComment(commentId, userId, true);
    }

    @Test
    void deleteCommentOnComment_Unauthorized() {
        // Setup
        mockSecurityContext();
        doThrow(new UnauthorizedAccessException("User not authorized to delete this reply"))
                .when(commentService).deleteComment(commentId, userId, true);

        // Execute
        ResponseEntity<PostResponse> response = commentController.deleteCommentOnComment(commentId);

        // Verify
        assertResponseNotNull(response);
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertFalse(Objects.requireNonNull(response.getBody()).isSuccess());
        assertEquals("User not authorized to delete this reply", response.getBody().getMessage());
        assertNull(response.getBody().getData());
        verify(commentService).deleteComment(commentId, userId, true);
    }

    @Test
    void deleteCommentOnComment_UnexpectedException() {
        // Setup
        mockSecurityContext();
        doThrow(new RuntimeException("Server error")).when(commentService).deleteComment(commentId, userId, true);

        // Execute
        ResponseEntity<PostResponse> response = commentController.deleteCommentOnComment(commentId);

        // Verify
        assertResponseNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertFalse(Objects.requireNonNull(response.getBody()).isSuccess());
        assertTrue(response.getBody().getMessage().contains("Server error"));
        assertNull(response.getBody().getData());
        verify(commentService).deleteComment(commentId, userId, true);
    }

    // -------------------- Edge Cases and Helper Method Tests --------------------

    @Test
    void createErrorResponse_ReturnsCorrectResponse() {
        // Setup - mock the service to throw an exception
        mockSecurityContext();
        when(commentService.createCommentOnPost(any())).thenThrow(new NullPointerException("Test exception"));

        // Execute - this should cause the controller's exception handling to create an
        // error response
        ResponseEntity<PostResponse> response = commentController.createCommentOnPost(validCommentRequest);

        // Verify
        assertResponseNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertFalse(Objects.requireNonNull(response.getBody()).isSuccess());
        assertTrue(response.getBody().getMessage().contains("Error processing request"));
        assertTrue(response.getBody().getMessage().contains("Test exception"));
        assertNull(response.getBody().getData());
    }

    @Test
    void test_SecurityContextNotAvailable() {
        // Test without mocking security context - should cause exception
        SecurityContextHolder.clearContext();

        // Execute - should handle NullPointerException from missing security context
        ResponseEntity<PostResponse> response = commentController.createCommentOnPost(validCommentRequest);

        // Verify error response was generated
        assertResponseNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertFalse(Objects.requireNonNull(response.getBody()).isSuccess());
        assertTrue(response.getBody().getMessage().contains("Error processing request"));
    }

    @Test
    void test_StaticSecurityContext() {
        // Test with static mocking of SecurityContextHolder
        try (MockedStatic<SecurityContextHolder> mockedStatic = mockStatic(SecurityContextHolder.class)) {
            // Setup
            mockedStatic.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(commentService.createCommentOnPost(any(CommentRequest.class))).thenReturn(commentOnPost);

            // Execute
            ResponseEntity<PostResponse> response = commentController.createCommentOnPost(validCommentRequest);

            // Verify
            assertResponseNotNull(response);
            assertEquals(HttpStatus.CREATED, response.getStatusCode());
            assertTrue(Objects.requireNonNull(response.getBody()).isSuccess());
        }
    }

    @Test
    void testGetCommentOnPost_success() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<CommentDTO> mockPage = new PageImpl<>(Collections.singletonList(commentOnPostDTO));

        SecurityContextHolder.setContext(securityContext);

        when(commentService.getCommentOnPost(postId, pageable)).thenReturn(mockPage);

        ResponseEntity<PostResponse> response = commentController.getCommentOnPost(postId, 0, 10);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(Objects.requireNonNull(response.getBody()).isSuccess());
        assertTrue(((Map<?, ?>) response.getBody().getData()).containsKey("content"));
    }

    @Test
    void testGetCommentOnComment_success() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<CommentDTO> mockPage = new PageImpl<>(Collections.singletonList(commentOnCommentDTO));

        SecurityContextHolder.setContext(securityContext);

        when(commentService.getCommentOnComment(commentId, pageable)).thenReturn(mockPage);

        ResponseEntity<PostResponse> response = commentController.getCommentOnComment(commentId, 0, 10);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(Objects.requireNonNull(response.getBody()).isSuccess());
        assertTrue(((Map<?, ?>) response.getBody().getData()).containsKey("content"));
    }

    @Test
    void testGetCommentOnPost_postNotFound_shouldReturnErrorResponse() {
        Pageable pageable = PageRequest.of(0, 10);

        SecurityContextHolder.setContext(securityContext);

        when(commentService.getCommentOnPost(postId, pageable))
                .thenThrow(new IllegalArgumentException("PostId is not found"));

        ResponseEntity<PostResponse> response = commentController.getCommentOnPost(postId, 0, 10);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Error processing request: PostId is not found", response.getBody().getMessage());
    }

    @Test
    void testGetCommentOnComment_commentNotFound_shouldReturnErrorResponse() {
        Pageable pageable = PageRequest.of(0, 10);

        SecurityContextHolder.setContext(securityContext);

        when(commentService.getCommentOnComment(commentId, pageable))
                .thenThrow(new IllegalArgumentException("CommentId is not found"));

        ResponseEntity<PostResponse> response = commentController.getCommentOnComment(commentId, 0, 10);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Error processing request: CommentId is not found", response.getBody().getMessage());
    }

}
