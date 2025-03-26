package com.safetypin.post.controller;

import com.safetypin.post.dto.PostCreateRequest;
import com.safetypin.post.dto.PostResponse;
import com.safetypin.post.exception.InvalidPostDataException;
import com.safetypin.post.exception.PostNotFoundException;
import com.safetypin.post.model.Category;
import com.safetypin.post.model.Post;
import com.safetypin.post.service.PostService;
import org.apache.hc.client5.http.auth.InvalidCredentialsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostControllerTest {

    @Mock
    private PostService postService;

    @InjectMocks
    private PostController postController;

    private Page<Map<String, Object>> mockPage;
    private Post mockPost;
    private PostCreateRequest validRequest;
    private String authorizationHeader;
    private UUID testUserId;

    @BeforeEach
    void setUp() {
        mockPage = new PageImpl<>(new ArrayList<>());
        testUserId = UUID.randomUUID();
        authorizationHeader = "Bearer test-token";

        Category mockCategory = new Category();
        mockCategory.setName("safety");

        mockPost = new Post();
        mockPost.setId(UUID.randomUUID());
        mockPost.setTitle("Test Post");
        mockPost.setCaption("Test Caption");
        mockPost.setLatitude(20.0);
        mockPost.setLongitude(10.0);
        mockPost.setCategory(mockCategory.getName());
        mockPost.setPostedBy(testUserId);

        validRequest = new PostCreateRequest();
        validRequest.setTitle("Test Post");
        validRequest.setCaption("Test Caption");
        validRequest.setLatitude(20.0);
        validRequest.setLongitude(10.0);
        validRequest.setCategory(mockCategory.getName());
    }

    /**
     * Test missing latitude parameter
     */
    @Test
    void whenGetPostsWithMissingLat_thenReturnBadRequest() {
        ResponseEntity<PostResponse> response = postController.getPosts(
                authorizationHeader, null, 10.0, 10.0, null, null, null, 0, 10);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        PostResponse errorResponse = response.getBody();

        assertNotNull(errorResponse);
        assertEquals("Latitude and longitude are required", errorResponse.getMessage());
    }

    /**
     * Test missing longitude parameter
     */
    @Test
    void whenGetPostsWithMissingLon_thenReturnBadRequest() {
        ResponseEntity<PostResponse> response = postController.getPosts(
                authorizationHeader, 10.0, null, 10.0, null, null, null, 0, 10);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        PostResponse errorResponse = response.getBody();

        assertNotNull(errorResponse);
        assertEquals("Latitude and longitude are required", errorResponse.getMessage());
    }

    /**
     * Test both lat and lon missing
     */
    @Test
    void whenGetPostsWithMissingLatAndLon_thenReturnBadRequest() {
        ResponseEntity<PostResponse> response = postController.getPosts(authorizationHeader, null, null, null, null, null, null, 0, 10);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        PostResponse errorResponse = response.getBody();

        assertNotNull(errorResponse);
        assertEquals("Latitude and longitude are required", errorResponse.getMessage());
    }

    /**
     * Test using default radius
     */
    @Test
    void whenGetPostsWithDefaultRadius_thenUseDefault() throws InvalidCredentialsException {
        Double lat = 20.0;
        Double lon = 10.0;
        Double radius = 10.0; // Default value
        int page = 0;
        int size = 10;
        Pageable pageable = PageRequest.of(page, size);

        when(postService.findPostsByLocation(lat, lon, radius, null, null, null, authorizationHeader, pageable))
                .thenReturn(mockPage);

        ResponseEntity<PostResponse> response = postController.getPosts(authorizationHeader, lat, lon, null, null, null, null, page, size);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(postService).findPostsByLocation(lat, lon, radius, null, null, null, authorizationHeader, pageable);
    }

    /**
     * Test with only dateFrom
     */
    @Test
    void whenGetPostsWithOnlyDateFrom_thenSetFromDateTime() throws InvalidCredentialsException {
        Double lat = 20.0;
        Double lon = 10.0;
        Double radius = 10.0;
        LocalDate dateFrom = LocalDate.now().minusDays(7);
        LocalDateTime fromDateTime = LocalDateTime.of(dateFrom, LocalTime.MIN);
        int page = 0;
        int size = 10;
        Pageable pageable = PageRequest.of(page, size);

        when(postService.findPostsByLocation(lat, lon, radius, null, fromDateTime, null, authorizationHeader, pageable))
                .thenReturn(mockPage);

        ResponseEntity<PostResponse> response = postController.getPosts(authorizationHeader, lat, lon, radius, null, dateFrom, null, page, size);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(postService).findPostsByLocation(lat, lon, radius, null, fromDateTime, null, authorizationHeader, pageable);
    }

    /**
     * Test with only dateTo
     */
    @Test
    void whenGetPostsWithOnlyDateTo_thenSetToDateTime() throws InvalidCredentialsException {
        Double lat = 20.0;
        Double lon = 10.0;
        Double radius = 10.0;
        LocalDate dateTo = LocalDate.now();
        LocalDateTime toDateTime = LocalDateTime.of(dateTo, LocalTime.MAX);
        int page = 0;
        int size = 10;
        Pageable pageable = PageRequest.of(page, size);

        when(postService.findPostsByLocation(lat, lon, radius, null, null, toDateTime, authorizationHeader, pageable))
                .thenReturn(mockPage);

        ResponseEntity<PostResponse> response = postController.getPosts(
                authorizationHeader, lat, lon, radius, null, null, dateTo, page, size);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(postService).findPostsByLocation(lat, lon, radius, null, null, toDateTime, authorizationHeader, pageable);
    }

    /**
     * Test custom pagination
     */
    @Test
    void whenGetPostsWithCustomPagination_thenUseCustomPageable() throws InvalidCredentialsException {
        Double lat = 20.0;
        Double lon = 10.0;
        int page = 2;
        int size = 25;
        Pageable pageable = PageRequest.of(page, size);

        when(postService.findPostsByLocation(lat, lon, 10.0, null, null, null, authorizationHeader, pageable))
                .thenReturn(mockPage);

        ResponseEntity<PostResponse> response = postController.getPosts(authorizationHeader, lat, lon, 10.0, null, null, null, page, size);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(postService).findPostsByLocation(lat, lon, 10.0, null, null, null, authorizationHeader, pageable);
    }

    /**
     * Test NumberFormatException handling
     */
    @Test
    void whenGetPostsAndNumberFormatExceptionThrown_thenReturnBadRequest() throws InvalidCredentialsException {
        Double lat = 20.0;
        Double lon = 10.0;
        when(postService.findPostsByLocation(anyDouble(), anyDouble(), anyDouble(), any(), any(), any(), anyString(), any()))
                .thenThrow(new NumberFormatException("Invalid number"));

        ResponseEntity<PostResponse> response = postController.getPosts(authorizationHeader, lat, lon, 10.0, null, null, null, 0, 10);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        PostResponse errorResponse = response.getBody();

        assertNotNull(errorResponse);
        assertEquals("Invalid location parameters", errorResponse.getMessage());
    }

    /**
     * Test general exception handling
     */
    @Test
    void whenGetPostsAndGeneralExceptionThrown_thenReturnInternalServerError() throws InvalidCredentialsException {
        Double lat = 20.0;
        Double lon = 10.0;
        String errorMessage = "Database connection failed";
        when(postService.findPostsByLocation(anyDouble(), anyDouble(), anyDouble(), any(), any(), any(), anyString(), any()))
                .thenThrow(new RuntimeException(errorMessage));

        ResponseEntity<PostResponse> response = postController.getPosts(authorizationHeader, lat, lon, 10.0, null, null, null, 0, 10);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        PostResponse errorResponse = response.getBody();

        assertNotNull(errorResponse);
        assertEquals("Error processing request: " + errorMessage, errorResponse.getMessage());
    }

    /**
     * Test MethodArgumentTypeMismatchException handler
     */
    @Test
    void handleArgumentTypeMismatchException_returnsCorrectErrorResponse() {
        MethodArgumentTypeMismatchException ex = mock(MethodArgumentTypeMismatchException.class);
        ResponseEntity<PostResponse> response = postController.handleArgumentTypeMismatch(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        PostResponse errorResponse = response.getBody();

        assertNotNull(errorResponse);
        assertEquals("Invalid location parameters", errorResponse.getMessage());
    }

    @Test
    void createPost_Success() {
        when(postService.createPost(
                anyString(), anyString(), anyDouble(), anyDouble(), anyString()))
                .thenReturn(mockPost);

        ResponseEntity<PostResponse> response = postController.createPost(validRequest);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertTrue(Objects.requireNonNull(response.getBody()).isSuccess());
        assertEquals("Post created successfully", response.getBody().getMessage());
        assertNotNull(response.getBody().getData());
    }

    @Test
    void createPost_ExceptionThrown() {
        when(postService.createPost(
                anyString(), anyString(), anyDouble(), anyDouble(), anyString()))
                .thenThrow(new InvalidPostDataException("Test exception"));

        ResponseEntity<PostResponse> response = postController.createPost(validRequest);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertFalse(Objects.requireNonNull(response.getBody()).isSuccess());
        assertEquals("Test exception", response.getBody().getMessage());
        assertNull(response.getBody().getData());
    }

    @Test
    void createPost_RuntimeException() {
        when(postService.createPost(
                anyString(), anyString(), anyDouble(), anyDouble(), anyString()))
                .thenThrow(new RuntimeException("Unexpected runtime exception"));

        ResponseEntity<PostResponse> response = postController.createPost(validRequest);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertFalse(Objects.requireNonNull(response.getBody()).isSuccess());
        assertEquals("Error processing request: Unexpected runtime exception", response.getBody().getMessage());
        assertNull(response.getBody().getData());
    }

    /**
     * Test for successful retrieval of all posts
     */
    @Test
    void findAll_Success() {
        int page = 0;
        int size = 10;
        Pageable pageable = PageRequest.of(page, size);

        List<Post> postList = new ArrayList<>();
        postList.add(mockPost);
        Page<Post> postPage = new PageImpl<>(postList, pageable, 1);

        when(postService.findAllPaginated(authorizationHeader, pageable)).thenReturn(postPage);

        ResponseEntity<PostResponse> response = postController.findAll(authorizationHeader, page, size);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        PostResponse postResponse = response.getBody();
        assert postResponse != null;
        assertTrue(postResponse.isSuccess());

        @SuppressWarnings("unchecked")
        Map<String, Object> responseData = (Map<String, Object>) postResponse.getData();
        assertEquals(1L, responseData.get("totalElements"));
        assertEquals(0, responseData.get("currentPage"));
        assertEquals(10, responseData.get("pageSize"));
    }

    /**
     * Test for exception during retrieval of all posts
     */
    @Test
    void findAll_ExceptionThrown() {
        int page = 0;
        int size = 10;
        String errorMessage = "Database error";

        when(postService.findAllPaginated(anyString(), any(Pageable.class)))
                .thenThrow(new RuntimeException(errorMessage));

        ResponseEntity<PostResponse> response = postController.findAll(authorizationHeader, page, size);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        PostResponse errorResponse = response.getBody();
        assertNotNull(errorResponse);
        assertFalse(errorResponse.isSuccess());
        assertEquals("Error processing request: " + errorMessage, errorResponse.getMessage());
        assertNull(errorResponse.getData());
    }

    /**
     * Test successful retrieval of posts feed by distance
     */
    @Test
    void getPostsFeedByDistance_Success() throws InvalidCredentialsException {
        int page = 0;
        int size = 10;
        Pageable pageable = PageRequest.of(page, size);

        List<Map<String, Object>> content = new ArrayList<>();
        Map<String, Object> postData = new HashMap<>();
        postData.put("post", new HashMap<>());
        content.add(postData);
        Page<Map<String, Object>> mockPage = new PageImpl<>(content, pageable, 1);

        when(postService.findPostsByDistanceFeed(anyDouble(), anyDouble(), anyString(), any(Pageable.class)))
                .thenReturn(mockPage);

        ResponseEntity<PostResponse> response = postController.getPostsFeedByDistance(
                authorizationHeader, 20.0, 10.0, page, size);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        PostResponse postResponse = response.getBody();
        assertNotNull(postResponse);
        assertTrue(postResponse.isSuccess());
        assertNotNull(postResponse.getData());
        verify(postService).findPostsByDistanceFeed(20.0, 10.0, authorizationHeader, pageable);
    }

    /**
     * Test missing latitude parameter
     */
    @Test
    void getPostsFeedByDistance_MissingLatitude() {
        // Act
        ResponseEntity<PostResponse> response = postController.getPostsFeedByDistance(authorizationHeader, null, 10.0, 0, 10);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        PostResponse errorResponse = response.getBody();
        assertNotNull(errorResponse);
        assertFalse(errorResponse.isSuccess());
        assertEquals("Latitude and longitude are required", errorResponse.getMessage());
        verifyNoInteractions(postService);
    }

    /**
     * Test missing longitude parameter
     */
    @Test
    void getPostsFeedByDistance_MissingLongitude() {
        // Act
        ResponseEntity<PostResponse> response = postController.getPostsFeedByDistance(authorizationHeader, 20.0, null, 0, 10);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        PostResponse errorResponse = response.getBody();
        assertNotNull(errorResponse);
        assertFalse(errorResponse.isSuccess());
        assertEquals("Latitude and longitude are required", errorResponse.getMessage());
        verifyNoInteractions(postService);
    }

    /**
     * Test service throwing InvalidPostDataException
     */
    @Test
    void getPostsFeedByDistance_InvalidPostDataException() throws InvalidCredentialsException {
        Double lat = 20.0;
        Double lon = 10.0;
        when(postService.findPostsByDistanceFeed(anyDouble(), anyDouble(), anyString(), any(Pageable.class)))
                .thenThrow(new InvalidPostDataException("Invalid post data"));

        ResponseEntity<PostResponse> response = postController.getPostsFeedByDistance(
                authorizationHeader, lat, lon, 0, 10);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        PostResponse errorResponse = response.getBody();
        assertNotNull(errorResponse);
        assertFalse(errorResponse.isSuccess());
        assertEquals("Invalid post data", errorResponse.getMessage());
    }

    /**
     * Test service throwing generic Exception
     */
    @Test
    void getPostsFeedByDistance_GenericException() throws InvalidCredentialsException {
        Double lat = 20.0;
        Double lon = 10.0;
        String errorMessage = "Database error";
        when(postService.findPostsByDistanceFeed(anyDouble(), anyDouble(), anyString(), any(Pageable.class)))
                .thenThrow(new RuntimeException(errorMessage));

        ResponseEntity<PostResponse> response = postController.getPostsFeedByDistance(
                authorizationHeader, lat, lon, 0, 10);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        PostResponse errorResponse = response.getBody();
        assertNotNull(errorResponse);
        assertFalse(errorResponse.isSuccess());
        assertEquals("Error processing request: " + errorMessage, errorResponse.getMessage());
    }

    /**
     * Test successful retrieval of posts feed by timestamp
     */
    @Test
    void getPostsFeedByTimestamp_Success() {
        // Arrange
        int page = 0;
        int size = 10;
        Pageable pageable = PageRequest.of(page, size);

        when(postService.findPostsByTimestampFeed(pageable))
                .thenReturn(mockPage);

        // Act
        ResponseEntity<PostResponse> response = postController.getPostsFeedByTimestamp(page, size);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        PostResponse postResponse = response.getBody();
        assertNotNull(postResponse);
        assertTrue(postResponse.isSuccess());
        assertNotNull(postResponse.getData());
        verify(postService).findPostsByTimestampFeed(pageable);
    }

    /**
     * Test successful search with both keyword and categories
     */
    @Test
    void getPostsFeedByTimestamp_Exception() {
        // Arrange
        int page = 0;
        int size = 10;
        String errorMessage = "Database error";

        when(postService.findPostsByTimestampFeed(any(Pageable.class)))
                .thenThrow(new RuntimeException(errorMessage));

        // Act
        ResponseEntity<PostResponse> response = postController.getPostsFeedByTimestamp(page, size);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        PostResponse errorResponse = response.getBody();
        assertNotNull(errorResponse);
        assertFalse(errorResponse.isSuccess());
        assertEquals("Error processing request: " + errorMessage, errorResponse.getMessage());
    }
}