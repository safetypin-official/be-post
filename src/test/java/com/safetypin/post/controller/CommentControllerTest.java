package com.safetypin.post.controller;

import com.safetypin.post.dto.CommentRequest;
import com.safetypin.post.dto.PostResponse;
import com.safetypin.post.dto.UserDetails;
import com.safetypin.post.exception.InvalidPostDataException;
import com.safetypin.post.exception.PostNotFoundException;
import com.safetypin.post.exception.UnauthorizedAccessException;
import com.safetypin.post.model.CommentOnComment;
import com.safetypin.post.model.CommentOnPost;
import com.safetypin.post.model.Post;
import com.safetypin.post.service.CommentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
    private Post parentPost;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        userDetails = new UserDetails("USER", true, userId, "Test User");

        // Setup comment request
        UUID parentId = UUID.randomUUID();
        validCommentRequest = new CommentRequest("This is a test comment", parentId);

        // Setup post with coordinates
        parentPost = Post.builder()
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
    }

    // Helper method to mock security context
    private void mockSecurityContext() {
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(securityContext.getAuthentication()).thenReturn(authentication);
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
        when(commentService.createCommentOnPost(eq(userId), any(CommentRequest.class))).thenReturn(commentOnPost);

        // Execute
        ResponseEntity<PostResponse> response = commentController.createCommentOnPost(validCommentRequest);

        // Verify
        assertResponseNotNull(response);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertTrue(response.getBody().isSuccess());
        assertEquals("Comment created successfully", response.getBody().getMessage());
        assertEquals(commentOnPost, response.getBody().getData());
        verify(commentService).createCommentOnPost(userId, validCommentRequest);
    }

    @Test
    void createCommentOnPost_PostNotFound() {
        // Setup
        mockSecurityContext();
        when(commentService.createCommentOnPost(eq(userId), any(CommentRequest.class)))
                .thenThrow(new PostNotFoundException("Post not found"));

        // Execute
        ResponseEntity<PostResponse> response = commentController.createCommentOnPost(validCommentRequest);

        // Verify
        assertResponseNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Post not found", response.getBody().getMessage());
        assertNull(response.getBody().getData());
        verify(commentService).createCommentOnPost(userId, validCommentRequest);
    }

    @Test
    void createCommentOnPost_InvalidData() {
        // Setup
        mockSecurityContext();
        when(commentService.createCommentOnPost(eq(userId), any(CommentRequest.class)))
                .thenThrow(new InvalidPostDataException("Invalid comment data"));

        // Execute
        ResponseEntity<PostResponse> response = commentController.createCommentOnPost(validCommentRequest);

        // Verify
        assertResponseNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Invalid comment data", response.getBody().getMessage());
        assertNull(response.getBody().getData());
        verify(commentService).createCommentOnPost(userId, validCommentRequest);
    }

    @Test
    void createCommentOnPost_UnexpectedException() {
        // Setup
        mockSecurityContext();
        when(commentService.createCommentOnPost(eq(userId), any(CommentRequest.class)))
                .thenThrow(new RuntimeException("Unexpected error"));

        // Execute
        ResponseEntity<PostResponse> response = commentController.createCommentOnPost(validCommentRequest);

        // Verify
        assertResponseNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertTrue(response.getBody().getMessage().contains("Unexpected error"));
        assertNull(response.getBody().getData());
        verify(commentService).createCommentOnPost(userId, validCommentRequest);
    }

    // -------------------- createCommentOnComment Tests --------------------

    @Test
    void createCommentOnComment_Success() {
        // Setup
        mockSecurityContext();
        when(commentService.createCommentOnComment(eq(userId), any(CommentRequest.class))).thenReturn(commentOnComment);

        // Execute
        ResponseEntity<PostResponse> response = commentController.createCommentOnComment(validCommentRequest);

        // Verify
        assertResponseNotNull(response);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertTrue(response.getBody().isSuccess());
        assertEquals("Comment created successfully", response.getBody().getMessage());
        assertEquals(commentOnComment, response.getBody().getData());
        verify(commentService).createCommentOnComment(userId, validCommentRequest);
    }

    @Test
    void createCommentOnComment_CommentNotFound() {
        // Setup
        mockSecurityContext();
        when(commentService.createCommentOnComment(eq(userId), any(CommentRequest.class)))
                .thenThrow(new PostNotFoundException("Parent comment not found"));

        // Execute
        ResponseEntity<PostResponse> response = commentController.createCommentOnComment(validCommentRequest);

        // Verify
        assertResponseNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Parent comment not found", response.getBody().getMessage());
        assertNull(response.getBody().getData());
        verify(commentService).createCommentOnComment(userId, validCommentRequest);
    }

    @Test
    void createCommentOnComment_InvalidData() {
        // Setup
        mockSecurityContext();
        when(commentService.createCommentOnComment(eq(userId), any(CommentRequest.class)))
                .thenThrow(new InvalidPostDataException("Invalid reply data"));

        // Execute
        ResponseEntity<PostResponse> response = commentController.createCommentOnComment(validCommentRequest);

        // Verify
        assertResponseNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Invalid reply data", response.getBody().getMessage());
        assertNull(response.getBody().getData());
        verify(commentService).createCommentOnComment(userId, validCommentRequest);
    }

    @Test
    void createCommentOnComment_UnexpectedException() {
        // Setup
        mockSecurityContext();
        when(commentService.createCommentOnComment(eq(userId), any(CommentRequest.class)))
                .thenThrow(new RuntimeException("Database connection failed"));

        // Execute
        ResponseEntity<PostResponse> response = commentController.createCommentOnComment(validCommentRequest);

        // Verify
        assertResponseNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertTrue(response.getBody().getMessage().contains("Database connection failed"));
        assertNull(response.getBody().getData());
        verify(commentService).createCommentOnComment(userId, validCommentRequest);
    }

    // -------------------- deleteCommentOnPost Tests --------------------

    @Test
    void deleteCommentOnPost_Success() {
        // Setup
        UUID commentId = UUID.randomUUID();
        mockSecurityContext();
        doNothing().when(commentService).deleteComment(commentId, userId, false);

        // Execute
        ResponseEntity<PostResponse> response = commentController.deleteCommentOnPost(commentId);

        // Verify
        assertResponseNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isSuccess());
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
        UUID commentId = UUID.randomUUID();
        mockSecurityContext();
        doThrow(new PostNotFoundException("Comment not found")).when(commentService).deleteComment(commentId, userId,
                false);

        // Execute
        ResponseEntity<PostResponse> response = commentController.deleteCommentOnPost(commentId);

        // Verify
        assertResponseNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Comment not found", response.getBody().getMessage());
        assertNull(response.getBody().getData());
        verify(commentService).deleteComment(commentId, userId, false);
    }

    @Test
    void deleteCommentOnPost_Unauthorized() {
        // Setup
        UUID commentId = UUID.randomUUID();
        mockSecurityContext();
        doThrow(new UnauthorizedAccessException("User not authorized to delete this comment"))
                .when(commentService).deleteComment(commentId, userId, false);

        // Execute
        ResponseEntity<PostResponse> response = commentController.deleteCommentOnPost(commentId);

        // Verify
        assertResponseNotNull(response);
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertEquals("User not authorized to delete this comment", response.getBody().getMessage());
        assertNull(response.getBody().getData());
        verify(commentService).deleteComment(commentId, userId, false);
    }

    @Test
    void deleteCommentOnPost_UnexpectedException() {
        // Setup
        UUID commentId = UUID.randomUUID();
        mockSecurityContext();
        doThrow(new RuntimeException("Database error")).when(commentService).deleteComment(commentId, userId, false);

        // Execute
        ResponseEntity<PostResponse> response = commentController.deleteCommentOnPost(commentId);

        // Verify
        assertResponseNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertTrue(response.getBody().getMessage().contains("Database error"));
        assertNull(response.getBody().getData());
        verify(commentService).deleteComment(commentId, userId, false);
    }

    // -------------------- deleteCommentOnComment Tests --------------------

    @Test
    void deleteCommentOnComment_Success() {
        // Setup
        UUID commentId = UUID.randomUUID();
        mockSecurityContext();
        doNothing().when(commentService).deleteComment(commentId, userId, true);

        // Execute
        ResponseEntity<PostResponse> response = commentController.deleteCommentOnComment(commentId);

        // Verify
        assertResponseNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isSuccess());
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
        UUID commentId = UUID.randomUUID();
        mockSecurityContext();
        doThrow(new PostNotFoundException("Reply not found")).when(commentService).deleteComment(commentId, userId,
                true);

        // Execute
        ResponseEntity<PostResponse> response = commentController.deleteCommentOnComment(commentId);

        // Verify
        assertResponseNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Reply not found", response.getBody().getMessage());
        assertNull(response.getBody().getData());
        verify(commentService).deleteComment(commentId, userId, true);
    }

    @Test
    void deleteCommentOnComment_Unauthorized() {
        // Setup
        UUID commentId = UUID.randomUUID();
        mockSecurityContext();
        doThrow(new UnauthorizedAccessException("User not authorized to delete this reply"))
                .when(commentService).deleteComment(commentId, userId, true);

        // Execute
        ResponseEntity<PostResponse> response = commentController.deleteCommentOnComment(commentId);

        // Verify
        assertResponseNotNull(response);
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertEquals("User not authorized to delete this reply", response.getBody().getMessage());
        assertNull(response.getBody().getData());
        verify(commentService).deleteComment(commentId, userId, true);
    }

    @Test
    void deleteCommentOnComment_UnexpectedException() {
        // Setup
        UUID commentId = UUID.randomUUID();
        mockSecurityContext();
        doThrow(new RuntimeException("Server error")).when(commentService).deleteComment(commentId, userId, true);

        // Execute
        ResponseEntity<PostResponse> response = commentController.deleteCommentOnComment(commentId);

        // Verify
        assertResponseNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertTrue(response.getBody().getMessage().contains("Server error"));
        assertNull(response.getBody().getData());
        verify(commentService).deleteComment(commentId, userId, true);
    }

    // -------------------- Edge Cases and Helper Method Tests --------------------

    @Test
    void createErrorResponse_ReturnsCorrectResponse() {
        // Setup - mock the service to throw an exception
        mockSecurityContext();
        when(commentService.createCommentOnPost(any(), any())).thenThrow(new NullPointerException("Test exception"));

        // Execute - this should cause the controller's exception handling to create an
        // error response
        ResponseEntity<PostResponse> response = commentController.createCommentOnPost(validCommentRequest);

        // Verify
        assertResponseNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
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
        assertFalse(response.getBody().isSuccess());
        assertTrue(response.getBody().getMessage().contains("Error processing request"));
    }

    @Test
    void test_StaticSecurityContext() {
        // Test with static mocking of SecurityContextHolder
        try (MockedStatic<SecurityContextHolder> mockedStatic = mockStatic(SecurityContextHolder.class)) {
            // Setup
            mockedStatic.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getPrincipal()).thenReturn(userDetails);
            when(commentService.createCommentOnPost(eq(userId), any(CommentRequest.class))).thenReturn(commentOnPost);

            // Execute
            ResponseEntity<PostResponse> response = commentController.createCommentOnPost(validCommentRequest);

            // Verify
            assertResponseNotNull(response);
            assertEquals(HttpStatus.CREATED, response.getStatusCode());
            assertTrue(response.getBody().isSuccess());
        }
    }

    // -------------------- Post Coordinate Validation Tests --------------------

    @Test
    void test_PostWithValidCoordinates() {
        // Setup a valid post with both latitude and longitude
        Post validPost = Post.builder()
                .title("Test Post")
                .caption("This is a test")
                .latitude(45.0)
                .longitude(90.0)
                .category("General")
                .build();

        // Verify coordinates were properly set
        assertEquals(45.0, validPost.getLatitude());
        assertEquals(90.0, validPost.getLongitude());
    }

    @Test
    void test_PostBuilder_MissingLatitude() {
        // Attempt to build a post with missing latitude
        Exception exception = assertThrows(IllegalArgumentException.class, () -> Post.builder()
                .title("Test Post")
                .caption("This is a test")
                .longitude(90.0) // Only longitude provided
                .category("General")
                .build());

        assertEquals("Both latitude and longitude must be provided", exception.getMessage());
    }

    @Test
    void test_PostBuilder_MissingLongitude() {
        // Attempt to build a post with missing longitude
        Exception exception = assertThrows(IllegalArgumentException.class, () -> Post.builder()
                .title("Test Post")
                .caption("This is a test")
                .latitude(45.0) // Only latitude provided
                .category("General")
                .build());

        assertEquals("Both latitude and longitude must be provided", exception.getMessage());
    }

    @Test
    void test_PostBuilder_MissingBothCoordinates() {
        // Attempt to build a post with neither latitude nor longitude
        Exception exception = assertThrows(IllegalArgumentException.class, () -> Post.builder()
                .title("Test Post")
                .caption("This is a test")
                .category("General")
                .build());

        assertEquals("Both latitude and longitude must be provided", exception.getMessage());
    }

    @Test
    void test_SetLatitude_NullValue() {
        // Setup
        Post post = Post.builder()
                .title("Test Post")
                .caption("Test content")
                .latitude(10.0)
                .longitude(20.0)
                .category("General")
                .build();

        // Attempt to set null latitude
        Exception exception = assertThrows(IllegalArgumentException.class, () -> post.setLatitude(null));

        assertEquals("Latitude cannot be null", exception.getMessage());
    }

    @Test
    void test_SetLongitude_NullValue() {
        // Setup
        Post post = Post.builder()
                .title("Test Post")
                .caption("Test content")
                .latitude(10.0)
                .longitude(20.0)
                .category("General")
                .build();

        // Attempt to set null longitude
        Exception exception = assertThrows(IllegalArgumentException.class, () -> post.setLongitude(null));

        assertEquals("Longitude cannot be null", exception.getMessage());
    }

    @Test
    void test_UpdateCoordinates_ValidValues() {
        // Setup
        Post post = Post.builder()
                .title("Test Post")
                .caption("Test content")
                .latitude(10.0)
                .longitude(20.0)
                .category("General")
                .build();

        // Update coordinates
        post.setLatitude(30.0);
        post.setLongitude(40.0);

        // Verify
        assertEquals(30.0, post.getLatitude());
        assertEquals(40.0, post.getLongitude());
    }

    @Test
    void test_EdgeCase_ExtremeCoordinates() {
        // Setup post with extreme but valid coordinates
        Post post = Post.builder()
                .title("Test Post")
                .caption("Test content")
                .latitude(90.0) // North Pole
                .longitude(180.0) // International Date Line
                .category("General")
                .build();

        // Verify
        assertEquals(90.0, post.getLatitude());
        assertEquals(180.0, post.getLongitude());
    }
}
