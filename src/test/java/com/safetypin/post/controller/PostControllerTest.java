package com.safetypin.post.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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

import com.safetypin.post.dto.FeedQueryDTO;
import com.safetypin.post.dto.PostCreateRequest;
import com.safetypin.post.dto.PostData;
import com.safetypin.post.dto.PostResponse;
import com.safetypin.post.dto.PostedByData;
import com.safetypin.post.dto.UserDetails;
import com.safetypin.post.exception.InvalidPostDataException;
import com.safetypin.post.exception.PostNotFoundException;
import com.safetypin.post.exception.UnauthorizedAccessException;
import com.safetypin.post.model.Post;
import com.safetypin.post.service.PostService;

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

    private PostedByData postedByData;

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

        postedByData = new PostedByData(testUserId, "Test User", "http://example.com/profile.jpg");
    }

    // ------------------- Find All Posts Tests -------------------

    @Test
    void findAll_success() {
        // Arrange
        List<Post> posts = Collections.singletonList(testPost);
        Page<Post> postsPage = new PageImpl<>(posts, pageable, posts.size());
        when(postService.findAllPaginated(any(Pageable.class))).thenReturn(postsPage);

        // Act
        ResponseEntity<PostResponse> response = postController.findAll(0, 10);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isSuccess());
        assertNotNull(response.getBody().getData());

        @SuppressWarnings("unchecked")
        Map<String, Object> responseData = (Map<String, Object>) response.getBody().getData();
        @SuppressWarnings("unchecked")
        List<PostData> postDataList = (List<PostData>) responseData.get("content");
        assertEquals(1, postDataList.size());
        PostData postData = postDataList.getFirst();
        assertEquals(testPost.getId(), postData.getId());

        verify(postService).findAllPaginated(any(Pageable.class));
    }

    @Test
    void findAll_emptyList() {
        // Arrange
        Page<Post> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);
        when(postService.findAllPaginated(any(Pageable.class))).thenReturn(emptyPage);

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
        when(postService.findAllPaginated(any(Pageable.class)))
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
    void getPostsFeedByDistance_withFilters() {
        // Arrange
        PostData postData = PostData.fromPostAndUserId(testPost, testUserId, postedByData);
        Map<String, Object> postMap = Map.of("post", postData, "distance", 2.5);
        List<Map<String, Object>> posts = Collections.singletonList(postMap);
        Page<Map<String, Object>> postsPage = new PageImpl<>(posts, pageable, posts.size());

        List<String> categories = List.of("DANGER");
        LocalDate from = LocalDate.now().minusDays(7);
        LocalDate to = LocalDate.now();

        // Capture the QueryDTO that will be created
        ArgumentCaptor<FeedQueryDTO> queryCaptor = ArgumentCaptor.forClass(FeedQueryDTO.class);

        when(postService.getFeed(queryCaptor.capture(), eq("distance")))
                .thenReturn(postsPage);

        // Act
        ResponseEntity<PostResponse> response = postController.getPostsFeedByDistance(
                40.7128, -74.0060, categories, "test", from, to, 0, 10);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isSuccess());

        // Verify the captured QueryDTO has the correct values
        FeedQueryDTO capturedQuery = queryCaptor.getValue();
        assertEquals(40.7128, capturedQuery.getUserLat());
        assertEquals(-74.0060, capturedQuery.getUserLon());
        assertEquals(categories, capturedQuery.getCategories());
        assertEquals("test", capturedQuery.getKeyword());
        assertEquals(LocalDateTime.of(from, LocalTime.MIN), capturedQuery.getDateFrom());
        assertEquals(LocalDateTime.of(to, LocalTime.MAX), capturedQuery.getDateTo());
        assertEquals(testUserId, capturedQuery.getUserId());
        assertEquals(0, capturedQuery.getPageable().getPageNumber());
        assertEquals(10, capturedQuery.getPageable().getPageSize());
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
        PostData postData = PostData.fromPostAndUserId(testPost, testUserId, postedByData);
        Map<String, Object> postMap = Map.of("post", postData, "distance", 2.5);
        List<Map<String, Object>> posts = Collections.singletonList(postMap);
        Page<Map<String, Object>> postsPage = new PageImpl<>(posts, pageable, posts.size());

        // Capture the QueryDTO that will be created
        ArgumentCaptor<FeedQueryDTO> queryCaptor = ArgumentCaptor.forClass(FeedQueryDTO.class);

        when(postService.getFeed(queryCaptor.capture(), eq("distance")))
                .thenReturn(postsPage);

        // Act
        ResponseEntity<PostResponse> response = postController.getPostsFeedByDistance(
                40.7128, -74.0060, null, "test", null, null, 0, 10);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isSuccess());

        // Verify the captured QueryDTO has the correct values
        FeedQueryDTO capturedQuery = queryCaptor.getValue();
        assertEquals(40.7128, capturedQuery.getUserLat());
        assertEquals(-74.0060, capturedQuery.getUserLon());
        assertNull(capturedQuery.getCategories());
        assertEquals("test", capturedQuery.getKeyword());
        assertNull(capturedQuery.getDateFrom());
        assertNull(capturedQuery.getDateTo());
        assertEquals(testUserId, capturedQuery.getUserId());
    }

    // ------------------- Get Posts Feed By Timestamp Tests -------------------

    @Test
    void getPostsFeedByTimestamp_success() {
        // Arrange
        PostData postData = PostData.fromPostAndUserId(testPost, testUserId, postedByData);
        Map<String, Object> postMap = Map.of("post", postData);
        List<Map<String, Object>> posts = Collections.singletonList(postMap);
        Page<Map<String, Object>> postsPage = new PageImpl<>(posts, pageable, posts.size());

        List<String> categories = List.of("DANGER");
        LocalDate from = LocalDate.now().minusDays(7);
        LocalDate to = LocalDate.now();

        // Capture the QueryDTO that will be created
        ArgumentCaptor<FeedQueryDTO> queryCaptor = ArgumentCaptor.forClass(FeedQueryDTO.class);

        when(postService.getFeed(queryCaptor.capture(), eq("timestamp")))
                .thenReturn(postsPage);

        // Act
        ResponseEntity<PostResponse> response = postController.getPostsFeedByTimestamp(
                categories, "test", from, to, 0, 10);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isSuccess());

        // Verify the captured QueryDTO has the correct values
        FeedQueryDTO capturedQuery = queryCaptor.getValue();
        assertNull(capturedQuery.getUserLat()); // Should be null for timestamp feed
        assertNull(capturedQuery.getUserLon()); // Should be null for timestamp feed
        assertEquals(categories, capturedQuery.getCategories());
        assertEquals("test", capturedQuery.getKeyword());
        assertEquals(LocalDateTime.of(from, LocalTime.MIN), capturedQuery.getDateFrom());
        assertEquals(LocalDateTime.of(to, LocalTime.MAX), capturedQuery.getDateTo());
        assertEquals(testUserId, capturedQuery.getUserId());
    }

    @Test
    void getPostsFeedByTimestamp_withNullDates() {
        // Arrange
        PostData postData = PostData.fromPostAndUserId(testPost, testUserId, postedByData);
        Map<String, Object> postMap = Map.of("post", postData);
        List<Map<String, Object>> posts = Collections.singletonList(postMap);
        Page<Map<String, Object>> postsPage = new PageImpl<>(posts, pageable, posts.size());

        List<String> categories = List.of("DANGER");

        // Capture the QueryDTO that will be created
        ArgumentCaptor<FeedQueryDTO> queryCaptor = ArgumentCaptor.forClass(FeedQueryDTO.class);

        when(postService.getFeed(queryCaptor.capture(), eq("timestamp")))
                .thenReturn(postsPage);

        // Act
        ResponseEntity<PostResponse> response = postController.getPostsFeedByTimestamp(
                categories, "test", null, null, 0, 10);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isSuccess());

        // Verify the captured QueryDTO has the correct values
        FeedQueryDTO capturedQuery = queryCaptor.getValue();
        assertNull(capturedQuery.getUserLat());
        assertNull(capturedQuery.getUserLon());
        assertEquals(categories, capturedQuery.getCategories());
        assertEquals("test", capturedQuery.getKeyword());
        assertNull(capturedQuery.getDateFrom());
        assertNull(capturedQuery.getDateTo());
        assertEquals(testUserId, capturedQuery.getUserId());
    }

    // ------------------- Get Posts Feed By Following Tests -------------------

    @Test
    void getPostsFeedByFollowing_success() {
        // Arrange
        PostData postData = PostData.fromPostAndUserId(testPost, testUserId, postedByData);
        Map<String, Object> postMap = Map.of("post", postData); // Following feed doesn't include distance
        List<Map<String, Object>> posts = Collections.singletonList(postMap);
        Page<Map<String, Object>> postsPage = new PageImpl<>(posts, pageable, posts.size());

        List<String> categories = List.of("INFO");
        LocalDate from = LocalDate.now().minusDays(5);
        LocalDate to = LocalDate.now();

        // Capture the QueryDTO that will be created
        ArgumentCaptor<FeedQueryDTO> queryCaptor = ArgumentCaptor.forClass(FeedQueryDTO.class);

        when(postService.getFeed(queryCaptor.capture(), eq("following")))
                .thenReturn(postsPage);

        // Act
        ResponseEntity<PostResponse> response = postController.getPostsFeedByFollowing(
                categories, "follow", from, to, 0, 10);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isSuccess());

        // Verify the captured QueryDTO has the correct values
        FeedQueryDTO capturedQuery = queryCaptor.getValue();
        assertNull(capturedQuery.getUserLat()); // Should be null for following feed
        assertNull(capturedQuery.getUserLon()); // Should be null for following feed
        assertEquals(categories, capturedQuery.getCategories());
        assertEquals("follow", capturedQuery.getKeyword());
        assertEquals(LocalDateTime.of(from, LocalTime.MIN), capturedQuery.getDateFrom());
        assertEquals(LocalDateTime.of(to, LocalTime.MAX), capturedQuery.getDateTo());
        assertEquals(testUserId, capturedQuery.getUserId());
        assertEquals(0, capturedQuery.getPageable().getPageNumber());
        assertEquals(10, capturedQuery.getPageable().getPageSize());

        verify(postService).getFeed(any(FeedQueryDTO.class), eq("following"));
    }

    @Test
    void getPostsFeedByFollowing_serviceThrowsException() {
        // Arrange
        List<String> categories = List.of("INFO");
        LocalDate from = LocalDate.now().minusDays(5);
        LocalDate to = LocalDate.now();

        when(postService.getFeed(any(FeedQueryDTO.class), eq("following")))
                .thenThrow(new RuntimeException("Following feed error"));

        // Act
        ResponseEntity<PostResponse> response = postController.getPostsFeedByFollowing(
                categories, "follow", from, to, 0, 10);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertNotNull(response.getBody().getMessage());
        assertTrue(response.getBody().getMessage().contains("Following feed error"));

        verify(postService).getFeed(any(FeedQueryDTO.class), eq("following"));
    }

    // ------------------- Get Posts By Specific User Tests -------------------

    @Test
    void getPostBySpecificUser_success() {
        // Arrange
        PostData postData = PostData.fromPostAndUserId(testPost, testUserId, postedByData);
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
        request.setImageUrl("http://example.com/image.jpg");

        // Capture the request that is passed to the service
        ArgumentCaptor<PostCreateRequest> requestCaptor = ArgumentCaptor.forClass(PostCreateRequest.class);

        when(postService.createPost(requestCaptor.capture())).thenReturn(testPost);

        // Act
        ResponseEntity<PostResponse> response = postController.createPost(request);

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertTrue(response.getBody().isSuccess());
        assertEquals("Post created successfully", response.getBody().getMessage());
        assertNotNull(response.getBody().getData());

        // Verify the request was properly passed with userId set
        PostCreateRequest capturedRequest = requestCaptor.getValue();
        assertEquals("New Post", capturedRequest.getTitle());
        assertEquals("New Content", capturedRequest.getCaption());
        assertEquals(40.7128, capturedRequest.getLatitude());
        assertEquals(-74.0060, capturedRequest.getLongitude());
        assertEquals("DANGER", capturedRequest.getCategory());
        assertEquals("http://example.com/image.jpg", capturedRequest.getImageUrl());
        assertEquals(testUserId, capturedRequest.getPostedBy());
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

        when(postService.createPost(any(PostCreateRequest.class)))
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

        when(postService.createPost(any(PostCreateRequest.class)))
                .thenThrow(new RuntimeException("Database error"));

        // Act
        ResponseEntity<PostResponse> response = postController.createPost(request);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertTrue(response.getBody().getMessage().contains("Database error"));
    }

    // Test that imageUrl is correctly passed to the service
    @Test
    void createPost_withImageUrl() {
        // Arrange
        PostCreateRequest request = new PostCreateRequest();
        request.setTitle("New Post");
        request.setCaption("New Content");
        request.setLatitude(40.7128);
        request.setLongitude(-74.0060);
        request.setCategory("DANGER");
        request.setImageUrl("http://example.com/image.jpg");

        ArgumentCaptor<PostCreateRequest> requestCaptor = ArgumentCaptor.forClass(PostCreateRequest.class);

        when(postService.createPost(requestCaptor.capture())).thenReturn(testPost);

        // Act
        ResponseEntity<PostResponse> response = postController.createPost(request);

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());

        // Verify imageUrl was correctly passed
        PostCreateRequest capturedRequest = requestCaptor.getValue();
        assertEquals("http://example.com/image.jpg", capturedRequest.getImageUrl());
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

        PostData postData = (PostData) response.getBody().getData();
        assertEquals(testPostId, postData.getId());
        assertEquals("Test Post", postData.getTitle());

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
        ResponseEntity<PostResponse> response = postController
                .handleArgumentTypeMismatch(typeMismatchException);

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
