package com.safetypin.post.controller;

import com.safetypin.post.dto.PostCreateRequest;
import com.safetypin.post.dto.PostResponse;
import com.safetypin.post.dto.UserDetails;
import com.safetypin.post.exception.InvalidPostDataException;
import com.safetypin.post.exception.InvalidCredentialsException;
import com.safetypin.post.exception.PostNotFoundException;
import com.safetypin.post.exception.UnauthorizedAccessException;
import com.safetypin.post.model.Post;
import com.safetypin.post.service.PostService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PostControllerTest {

    @Mock
    private PostService postService;

    @Mock
    private Authentication authentication;

    @Mock
    private SecurityContext securityContext;

    @InjectMocks
    private PostController postController;

    private UUID testUserId;
    private UUID testPostId;
    private Post testPost;
    private UserDetails userDetails;
    private Pageable pageable;
    private MethodArgumentTypeMismatchException typeMismatchException;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Setup Security Context
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        // Setup User
        testUserId = UUID.randomUUID();
        userDetails = mock(UserDetails.class);
        when(userDetails.getUserId()).thenReturn(testUserId);
        when(authentication.getPrincipal()).thenReturn(userDetails);

        // Setup Post
        testPostId = UUID.randomUUID();
        testPost = new Post.Builder()
                .id(testPostId)
                .title("Test Post")
                .caption("Test Content")
                .location(40.7128, -74.0060)
                .category("DANGER")
                .postedBy(testUserId)
                .build();

        // Setup Pageable
        pageable = PageRequest.of(0, 10);

        // Setup Exception for exception handler test
        typeMismatchException = mock(MethodArgumentTypeMismatchException.class);
    }

    // ------------------- Find All Posts Tests -------------------

    @Test
    void findAll_success() {
        // Arrange
        List<Post> posts = Collections.singletonList(testPost);
        Page<Post> postsPage = new PageImpl<>(posts, pageable, posts.size());
        when(postService.findAllPaginated(any(UUID.class), any(Pageable.class))).thenReturn(postsPage);

        // Act
        ResponseEntity<PostResponse> response = postController.findAll(0, 10);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isSuccess());
        assertNotNull(response.getBody().getData());

        @SuppressWarnings("unchecked")
        Map<String, Object> responseData = (Map<String, Object>) response.getBody().getData();
        assertEquals(1, ((List<?>) responseData.get("content")).size());

        verify(postService).findAllPaginated(eq(testUserId), any(Pageable.class));
    }

    @Test
    void findAll_emptyList() {
        // Arrange
        Page<Post> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);
        when(postService.findAllPaginated(any(UUID.class), any(Pageable.class))).thenReturn(emptyPage);

        // Act
        ResponseEntity<PostResponse> response = postController.findAll(0, 10);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isSuccess());

        @SuppressWarnings("unchecked")
        Map<String, Object> responseData = (Map<String, Object>) response.getBody().getData();
        assertEquals(0, ((List<?>) responseData.get("content")).size());
    }

    @Test
    void findAll_serviceThrowsException() {
        // Arrange
        when(postService.findAllPaginated(any(UUID.class), any(Pageable.class)))
                .thenThrow(new RuntimeException("Test error"));

        // Act
        ResponseEntity<PostResponse> response = postController.findAll(0, 10);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertNotNull(response.getBody().getMessage());
        assertTrue(response.getBody().getMessage().contains("Test error"));
    }

    // ------------------- Get Posts Feed By Distance Tests -------------------



    @Test
    void getPostsFeedByDistance_withFilters() throws InvalidCredentialsException {
        // Arrange
        Map<String, Object> postData = new HashMap<>();
        postData.put("post", Map.of("title", "Test Post"));
        postData.put("distance", 2.5);
        List<Map<String, Object>> posts = Collections.singletonList(postData);
        Page<Map<String, Object>> postsPage = new PageImpl<>(posts, pageable, posts.size());

        List<String> categories = List.of("DANGER");
        LocalDate from = LocalDate.now().minusDays(7);
        LocalDate to = LocalDate.now();

        when(postService.findPostsByDistanceFeed(
                anyDouble(), anyDouble(), anyList(), anyString(),
                any(), any(), any(UUID.class), any(Pageable.class)))
                .thenReturn(postsPage);

        // Act
        ResponseEntity<PostResponse> response = postController.getPostsFeedByDistance(
                40.7128, -74.0060, categories, "test", from, to, 0, 10);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isSuccess());
    }

    @Test
    void getPostsFeedByDistance_nullCoordinates() {
        // Act
        ResponseEntity<PostResponse> response = postController.getPostsFeedByDistance(
                null, null, null, null, null, null, 0, 10);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertTrue(response.getBody().getMessage().contains("Latitude and longitude are required"));
    }

    // ------------------- Get Posts Feed By Timestamp Tests -------------------

    @Test
    void getPostsFeedByTimestamp_success() throws InvalidCredentialsException {
        // Arrange
        Map<String, Object> postData = new HashMap<>();
        postData.put("post", Map.of("title", "Test Post"));
        List<Map<String, Object>> posts = Collections.singletonList(postData);
        Page<Map<String, Object>> postsPage = new PageImpl<>(posts, pageable, posts.size());
        List<String> categories = List.of("DANGER");
        LocalDate from = LocalDate.now().minusDays(7);
        LocalDate to = LocalDate.now();

        // Mock the service to return postsPage when parameters match
        when(postService.findPostsByTimestampFeed(
                eq(categories), eq("test"),
                eq(LocalDateTime.of(from, LocalTime.MIN)),
                eq(LocalDateTime.of(to, LocalTime.MAX)),
                eq(testUserId), any(Pageable.class)))
                .thenReturn(postsPage);

        // Act
        ResponseEntity<PostResponse> response = postController.getPostsFeedByTimestamp(
                categories, "test", from, to, 0, 10);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isSuccess());
    }

    @Test
    void getPostsFeedByTimestamp_withFilters() throws InvalidCredentialsException {
        // Arrange
        Map<String, Object> postData = new HashMap<>();
        postData.put("post", Map.of("title", "Test Post"));
        List<Map<String, Object>> posts = Collections.singletonList(postData);
        Page<Map<String, Object>> postsPage = new PageImpl<>(posts, pageable, posts.size());

        List<String> categories = List.of("DANGER");
        LocalDate from = LocalDate.now().minusDays(7);
        LocalDate to = LocalDate.now();

        when(postService.findPostsByTimestampFeed(
                anyList(), anyString(), any(), any(), any(UUID.class), any(Pageable.class)))
                .thenReturn(postsPage);

        // Act
        ResponseEntity<PostResponse> response = postController.getPostsFeedByTimestamp(
                categories, "test", from, to, 0, 10);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isSuccess());
    }

    // ------------------- Create Post Tests -------------------

    @Test
    void createPost_success() {
        // Arrange
        PostCreateRequest request = new PostCreateRequest();
        request.setTitle("New Post");
        request.setCaption("New Content");
        request.setLatitude(40.7128);
        request.setLongitude(-74.0060);
        request.setCategory("DANGER");

        when(postService.createPost(
                anyString(), anyString(), anyDouble(), anyDouble(), anyString(), any(UUID.class)))
                .thenReturn(testPost);

        // Act
        ResponseEntity<PostResponse> response = postController.createPost(request);

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertTrue(response.getBody().isSuccess());
        assertEquals("Post created successfully", response.getBody().getMessage());
        assertNotNull(response.getBody().getData());

        verify(postService).createPost(
                eq("New Post"), eq("New Content"), eq(40.7128), eq(-74.0060),
                eq("DANGER"), eq(testUserId));
    }

    @Test
    void createPost_invalidData() {
        // Arrange
        PostCreateRequest request = new PostCreateRequest();
        request.setTitle("");
        request.setCaption("Test Content");
        request.setLatitude(40.7128);
        request.setLongitude(-74.0060);
        request.setCategory("DANGER");

        when(postService.createPost(
                anyString(), anyString(), anyDouble(), anyDouble(), anyString(), any(UUID.class)))
                .thenThrow(new InvalidPostDataException("Title is required"));

        // Act
        ResponseEntity<PostResponse> response = postController.createPost(request);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Title is required", response.getBody().getMessage());
    }

    @Test
    void createPost_internalError() {
        // Arrange
        PostCreateRequest request = new PostCreateRequest();
        request.setTitle("New Post");
        request.setCaption("New Content");
        request.setLatitude(40.7128);
        request.setLongitude(-74.0060);
        request.setCategory("DANGER");

        when(postService.createPost(
                anyString(), anyString(), anyDouble(), anyDouble(), anyString(), any(UUID.class)))
                .thenThrow(new RuntimeException("Database error"));

        // Act
        ResponseEntity<PostResponse> response = postController.createPost(request);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertTrue(response.getBody().getMessage().contains("Database error"));
    }

    // ------------------- Get Post By ID Tests -------------------

    @Test
    void getPostById_success() {
        // Arrange
        when(postService.findById(any(UUID.class))).thenReturn(testPost);

        // Act
        ResponseEntity<PostResponse> response = postController.getPostById(testPostId);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isSuccess());
        assertNotNull(response.getBody().getData());

        @SuppressWarnings("unchecked")
        Map<String, Object> postData = (Map<String, Object>) response.getBody().getData();
        assertEquals(testPostId, postData.get("id"));
        assertEquals("Test Post", postData.get("title"));

        verify(postService).findById(eq(testPostId));
    }

    @Test
    void getPostById_notFound() {
        // Arrange
        when(postService.findById(any(UUID.class)))
                .thenThrow(new PostNotFoundException("Post not found"));

        // Act
        ResponseEntity<PostResponse> response = postController.getPostById(testPostId);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Post not found", response.getBody().getMessage());
    }

    // ------------------- Delete Post Tests -------------------

    @Test
    void deletePost_success() {
        // Arrange
        doNothing().when(postService).deletePost(any(UUID.class), any(UUID.class));

        // Act
        ResponseEntity<PostResponse> response = postController.deletePost(testPostId);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isSuccess());
        assertEquals("Post deleted successfully", response.getBody().getMessage());

        @SuppressWarnings("unchecked")
        Map<String, Object> responseData = (Map<String, Object>) response.getBody().getData();
        assertEquals(testPostId, responseData.get("postId"));
        assertEquals(testUserId, responseData.get("deletedBy"));

        verify(postService).deletePost(eq(testPostId), eq(testUserId));
    }

    @Test
    void deletePost_notAuthorized() {
        // Arrange
        doThrow(new UnauthorizedAccessException("User not authorized"))
                .when(postService).deletePost(any(UUID.class), any(UUID.class));

        // Act
        ResponseEntity<PostResponse> response = postController.deletePost(testPostId);

        // Assert
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertEquals("User not authorized", response.getBody().getMessage());
    }

    @Test
    void deletePost_notFound() {
        // Arrange
        doThrow(new PostNotFoundException("Post not found"))
                .when(postService).deletePost(any(UUID.class), any(UUID.class));

        // Act
        ResponseEntity<PostResponse> response = postController.deletePost(testPostId);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Post not found", response.getBody().getMessage());
    }

    // ------------------- Exception Handler Tests -------------------

    @Test
    void handleArgumentTypeMismatch() {
        // Act
        ResponseEntity<PostResponse> response = postController.handleArgumentTypeMismatch(typeMismatchException);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Invalid location parameters", response.getBody().getMessage());
    }

    // ------------------- Edge Cases and Additional Tests -------------------



    @Test
    void getPostsFeedByDistance_edgeCase_numberFormatException() throws InvalidCredentialsException {
        // Arrange
        // Simulate invalid latitude/longitude parameters (e.g., passing strings instead
        // of numbers)
        // The controller's validateLocationParams will throw InvalidPostDataException
        when(postService.findPostsByDistanceFeed(anyDouble(), anyDouble(), anyList(), anyString(), any(), any(),
                any(UUID.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList())); // Avoid service exception

        // Act
        ResponseEntity<PostResponse> response = postController.getPostsFeedByDistance(
                null, null, null, null, null, null, 0, 10);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertTrue(response.getBody().getMessage().contains("Latitude and longitude are required"));
    }

    @Test
    void createPost_edgeCase_nullRequest() {
        // Act
        ResponseEntity<PostResponse> response = postController.createPost(null);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertTrue(response.getBody().getMessage().contains("Error processing request"));
    }
}
