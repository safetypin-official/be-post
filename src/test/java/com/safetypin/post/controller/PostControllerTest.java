package com.safetypin.post.controller;

import com.safetypin.post.dto.PostCreateRequest;
import com.safetypin.post.dto.PostResponse;
import com.safetypin.post.dto.PostData;
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
        UserDetails userDetails = mock(UserDetails.class);
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
        PostData postData = PostData.fromPostAndUserId(testPost, testUserId);
        Map<String, Object> postMap = Map.of("post", postData, "distance", 2.5);
        List<Map<String, Object>> posts = Collections.singletonList(postMap);
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
    void getPostsFeedByDistance_nullLatitude() {
        // Act
        ResponseEntity<PostResponse> response = postController.getPostsFeedByDistance(
                null, 10.0, null, null, null, null, 0, 10);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Latitude and longitude are required", response.getBody().getMessage());
    }

    @Test
    void getPostsFeedByDistance_nullLongitude() {
        // Act
        ResponseEntity<PostResponse> response = postController.getPostsFeedByDistance(
                10.0, null, null, null, null, null, 0, 10);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Latitude and longitude are required", response.getBody().getMessage());
    }

    @Test
    void getPostsFeedByDistance_withNullDates() {
        // Arrange
        PostData postData = PostData.fromPostAndUserId(testPost, testUserId);
        Map<String, Object> postMap = Map.of("post", postData, "distance", 2.5);
        List<Map<String, Object>> posts = Collections.singletonList(postMap);
        Page<Map<String, Object>> postsPage = new PageImpl<>(posts, pageable, posts.size());

        // Test case where both dates are null
        when(postService.findPostsByDistanceFeed(
                eq(40.7128), eq(-74.0060), isNull(), eq("test"),
                isNull(), isNull(), eq(testUserId), eq(pageable)))
                .thenReturn(postsPage);

        // Act
        ResponseEntity<PostResponse> response = postController.getPostsFeedByDistance(
                40.7128, -74.0060, null, "test", null, null, 0, 10);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isSuccess());
        verify(postService).findPostsByDistanceFeed(
                eq(40.7128), eq(-74.0060), isNull(), eq("test"),
                isNull(), isNull(), eq(testUserId), eq(pageable));
    }


    // ------------------- Get Posts Feed By Timestamp Tests -------------------

    @Test
    void getPostsFeedByTimestamp_success() throws InvalidCredentialsException {
        // Arrange
        PostData postData = PostData.fromPostAndUserId(testPost, testUserId);
        Map<String, Object> postMap = Map.of("post", postData);
        List<Map<String, Object>> posts = Collections.singletonList(postMap);
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
        PostData postData = PostData.fromPostAndUserId(testPost, testUserId);
        Map<String, Object> postMap = Map.of("post", postData);
        List<Map<String, Object>> posts = Collections.singletonList(postMap);
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

    @Test
    void getPostsFeedByTimestamp_withNullDates() {
        // Arrange
        PostData postData = PostData.fromPostAndUserId(testPost, testUserId);
        Map<String, Object> postMap = Map.of("post", postData);
        List<Map<String, Object>> posts = Collections.singletonList(postMap);
        Page<Map<String, Object>> postsPage = new PageImpl<>(posts, pageable, posts.size());

        List<String> categories = List.of("DANGER");

        when(postService.findPostsByTimestampFeed(
                eq(categories), eq("test"), isNull(), isNull(),
                eq(testUserId), eq(pageable)))
                .thenReturn(postsPage);

        // Act
        ResponseEntity<PostResponse> response = postController.getPostsFeedByTimestamp(
                categories, "test", null, null, 0, 10);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isSuccess());
        verify(postService).findPostsByTimestampFeed(
                eq(categories), eq("test"), isNull(), isNull(),
                eq(testUserId), eq(pageable));
    }

    // ------------------- Get Posts By Specific User Tests -------------------

    @Test
    void getPostBySpecificUser_success() {
        // Arrange
        PostData postData = PostData.fromPostAndUserId(testPost, testUserId);
        Map<String, Object> postMap = Map.of("post", postData);
        List<Map<String, Object>> posts = List.of(postMap);
        Page<Map<String, Object>> postsPage = new PageImpl<>(posts, pageable, posts.size());

        when(postService.findPostsByUser(eq(testUserId), any(Pageable.class)))
                .thenReturn(postsPage);

        // Act
        ResponseEntity<PostResponse> response = postController.getPostBySpecificUser(
                testUserId, 0, 10);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isSuccess());
        assertNotNull(response.getBody().getData());

        @SuppressWarnings("unchecked")
        Map<String, Object> responseData = (Map<String, Object>) response.getBody().getData();
        assertEquals(1, ((List<?>) responseData.get("content")).size());

        verify(postService).findPostsByUser(eq(testUserId), any(Pageable.class));
    }

    @Test
    void getPostBySpecificUser_emptyResult() {
        // Arrange
        UUID postUserId = UUID.randomUUID();
        Page<Map<String, Object>> emptyPage = new PageImpl<>(
                List.of(), pageable, 0);

        when(postService.findPostsByUser(eq(postUserId), any(Pageable.class)))
                .thenReturn(emptyPage);

        // Act
        ResponseEntity<PostResponse> response = postController.getPostBySpecificUser(
                postUserId, 0, 10);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isSuccess());

        @SuppressWarnings("unchecked")
        Map<String, Object> responseData = (Map<String, Object>) response.getBody().getData();
        assertEquals(0, ((List<?>) responseData.get("content")).size());
        assertEquals(0L, responseData.get("totalElements"));
    }

    @Test
    void getPostBySpecificUser_nullUserId() {
        // Act
        ResponseEntity<PostResponse> response = postController.getPostBySpecificUser(
                null, 0, 10);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertTrue(response.getBody().getMessage().contains("User ID is required"));
    }

    @Test
    void getPostBySpecificUser_serviceError() {
        // Arrange
        UUID postUserId = UUID.randomUUID();
        when(postService.findPostsByUser(any(UUID.class), any(Pageable.class)))
                .thenThrow(new RuntimeException("Database error"));

        // Act
        ResponseEntity<PostResponse> response = postController.getPostBySpecificUser(
                postUserId, 0, 10);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertTrue(response.getBody().getMessage().contains("Database error"));
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
                "New Post", "New Content", 40.7128, -74.0060,
                "DANGER", testUserId);
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
        PostData postData = PostData.fromPostAndUserId(testPost, testUserId);
        when(postService.findById(any(UUID.class))).thenReturn(testPost);

        // Act
        ResponseEntity<PostResponse> response = postController.getPostById(testPostId);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isSuccess());
        assertNotNull(response.getBody().getData());

        PostData responseData = (PostData) response.getBody().getData();
        assertEquals(postData, responseData);

        verify(postService).findById(testPostId);
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

        verify(postService).deletePost(testPostId, testUserId);
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
    void createPost_edgeCase_nullRequest() {
        // Act
        ResponseEntity<PostResponse> response = postController.createPost(null);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertTrue(response.getBody().getMessage().contains("Error processing request"));
    }
}
