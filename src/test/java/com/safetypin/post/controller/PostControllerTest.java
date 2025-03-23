package com.safetypin.post.controller;

import com.safetypin.post.dto.PostCreateRequest;
import com.safetypin.post.dto.PostResponse;
import com.safetypin.post.exception.InvalidPostDataException;
import com.safetypin.post.model.Category;
import com.safetypin.post.model.Post;
import com.safetypin.post.service.PostService;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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

    @BeforeEach
    void setUp() {
        mockPage = new PageImpl<>(new ArrayList<>());

        Category mockCategory = new Category();
        mockCategory.setName("safety");

        mockPost = new Post();
        mockPost.setId(UUID.randomUUID());
        mockPost.setTitle("Test Post");
        mockPost.setCaption("Test Caption");
        mockPost.setLatitude(20.0);
        mockPost.setLongitude(10.0);
        mockPost.setCategory(mockCategory.getName()); // Changed from Category to String

        validRequest = new PostCreateRequest();
        validRequest.setTitle("Test Post");
        validRequest.setCaption("Test Caption");
        validRequest.setLatitude(20.0);
        validRequest.setLongitude(10.0);
        validRequest.setCategory(mockCategory.getName()); // Changed from Category to String
    }

    /**
     * Test missing latitude parameter
     */
    @Test
    void whenGetPostsWithMissingLat_thenReturnBadRequest() {
        ResponseEntity<PostResponse> response = postController.getPosts(
                null, 10.0, 10.0, null, null, null, 0, 10);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        PostResponse errorResponse = response.getBody();

        assertEquals("Latitude and longitude are required", errorResponse.getMessage());
    }

    /**
     * Test missing longitude parameter
     */
    @Test
    void whenGetPostsWithMissingLon_thenReturnBadRequest() {
        ResponseEntity<PostResponse> response = postController.getPosts(
                20.0, null, 10.0, null, null, null, 0, 10);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        PostResponse errorResponse = response.getBody();

        assertEquals("Latitude and longitude are required", errorResponse.getMessage());
    }

    /**
     * Test both lat and lon missing
     */
    @Test
    void whenGetPostsWithMissingLatAndLon_thenReturnBadRequest() {
        ResponseEntity<PostResponse> response = postController.getPosts(null, null, 10.0, null, null, null, 0, 10);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        PostResponse errorResponse = response.getBody();

        assertEquals("Latitude and longitude are required", errorResponse.getMessage());
    }

    /**
     * Test using default radius
     */
    @Test
    void whenGetPostsWithDefaultRadius_thenUseDefault() {
        Double lat = 20.0;
        Double lon = 10.0;
        Double radius = 10.0; // Default value
        int page = 0;
        int size = 10;
        Pageable pageable = PageRequest.of(page, size);

        when(postService.findPostsByLocation(lat, lon, radius, null, null, null, pageable))
                .thenReturn(mockPage);

        ResponseEntity<PostResponse> response = postController.getPosts(lat, lon, null, null, null, null, page, size);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(postService).findPostsByLocation(lat, lon, radius, null, null, null, pageable);
    }

    /**
     * Test with only dateFrom
     */
    @Test
    void whenGetPostsWithOnlyDateFrom_thenSetFromDateTime() {
        Double lat = 20.0;
        Double lon = 10.0;
        Double radius = 10.0;
        LocalDate dateFrom = LocalDate.now().minusDays(7);
        LocalDateTime fromDateTime = LocalDateTime.of(dateFrom, LocalTime.MIN);
        int page = 0;
        int size = 10;
        Pageable pageable = PageRequest.of(page, size);

        when(postService.findPostsByLocation(lat, lon, radius, null, fromDateTime, null, pageable))
                .thenReturn(mockPage);

        ResponseEntity<PostResponse> response = postController.getPosts(lat, lon, radius, null, dateFrom, null, page, size);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(postService).findPostsByLocation(lat, lon, radius, null, fromDateTime, null, pageable);
    }

    /**
     * Test with only dateTo
     */
    @Test
    void whenGetPostsWithOnlyDateTo_thenSetToDateTime() {
        Double lat = 20.0;
        Double lon = 10.0;
        Double radius = 10.0;
        LocalDate dateTo = LocalDate.now();
        LocalDateTime toDateTime = LocalDateTime.of(dateTo, LocalTime.MAX);
        int page = 0;
        int size = 10;
        Pageable pageable = PageRequest.of(page, size);

        when(postService.findPostsByLocation(lat, lon, radius, null, null, toDateTime, pageable))
                .thenReturn(mockPage);

        ResponseEntity<PostResponse> response = postController.getPosts(lat, lon, radius, null, null, dateTo, page, size);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(postService).findPostsByLocation(lat, lon, radius, null, null, toDateTime, pageable);
    }

    /**
     * Test custom pagination
     */
    @Test
    void whenGetPostsWithCustomPagination_thenUseCustomPageable() {
        Double lat = 20.0;
        Double lon = 10.0;
        int page = 2;
        int size = 25;
        Pageable pageable = PageRequest.of(page, size);

        when(postService.findPostsByLocation(lat, lon, 10.0, null, null, null, pageable))
                .thenReturn(mockPage);

        ResponseEntity<PostResponse> response = postController.getPosts(lat, lon, 10.0, null, null, null, page, size);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(postService).findPostsByLocation(lat, lon, 10.0, null, null, null, pageable);
    }

    /**
     * Test NumberFormatException handling
     */
    @Test
    void whenGetPostsAndNumberFormatExceptionThrown_thenReturnBadRequest() {
        Double lat = 20.0;
        Double lon = 10.0;
        when(postService.findPostsByLocation(anyDouble(), anyDouble(), anyDouble(), any(), any(), any(), any()))
                .thenThrow(new NumberFormatException("Invalid number"));

        ResponseEntity<PostResponse> response = postController.getPosts(lat, lon, 10.0, null, null, null, 0, 10);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        PostResponse errorResponse = response.getBody();

        assertEquals("Invalid location parameters", errorResponse.getMessage());
    }

    /**
     * Test general exception handling
     */
    @Test
    void whenGetPostsAndGeneralExceptionThrown_thenReturnInternalServerError() {
        Double lat = 20.0;
        Double lon = 10.0;
        String errorMessage = "Database connection failed";
        when(postService.findPostsByLocation(anyDouble(), anyDouble(), anyDouble(), any(), any(), any(), any()))
                .thenThrow(new RuntimeException(errorMessage));

        ResponseEntity<PostResponse> response = postController.getPosts(lat, lon, 10.0, null, null, null, 0, 10);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        PostResponse errorResponse = response.getBody();

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

        assertEquals("Invalid location parameters", errorResponse.getMessage());
    }

    @Test
    void createPost_Success() {
        // Arrange
        when(postService.createPost(
                anyString(), anyString(), anyDouble(), anyDouble(), any()))
                .thenReturn(mockPost);

        // Act
        ResponseEntity<PostResponse> response = postController.createPost(validRequest);

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertTrue(response.getBody().isSuccess());
        assertEquals("Post created successfully", response.getBody().getMessage());
        assertNotNull(response.getBody().getData());
    }

    @Test
    void createPost_ExceptionThrown() {
        // Arrange
        when(postService.createPost(
                anyString(), anyString(), anyDouble(), anyDouble(), any()))
                .thenThrow(new InvalidPostDataException("Test exception"));

        // Act
        ResponseEntity<PostResponse> response = postController.createPost(validRequest);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Test exception", response.getBody().getMessage());
        assertNull(response.getBody().getData());
    }

    @Test
    void createPost_RuntimeException() {
        // Arrange
        when(postService.createPost(
                anyString(), anyString(), anyDouble(), anyDouble(), any()))
                .thenThrow(new RuntimeException("Unexpected runtime exception"));

        // Act
        ResponseEntity<PostResponse> response = postController.createPost(validRequest);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Error processing request: Unexpected runtime exception", response.getBody().getMessage());
        assertNull(response.getBody().getData());
    }

    /**
     * Test for successful retrieval of all posts
     */
    @Test
    void findAll_Success() {
        // Arrange
        int page = 0;
        int size = 10;
        Pageable pageable = PageRequest.of(page, size);

        List<Post> postList = new ArrayList<>();
        postList.add(mockPost);
        Page<Post> postPage = new PageImpl<>(postList, pageable, 1);

        when(postService.findAllPaginated(pageable)).thenReturn(postPage);

        // Act
        ResponseEntity<PostResponse> response = postController.findAll(page, size);

        // Assert
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
        // Arrange
        int page = 0;
        int size = 10;
        String errorMessage = "Database error";

        when(postService.findAllPaginated(any(Pageable.class)))
                .thenThrow(new RuntimeException(errorMessage));

        // Act
        ResponseEntity<PostResponse> response = postController.findAll(page, size);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        PostResponse errorResponse = response.getBody();
        assertFalse(errorResponse.isSuccess());
        assertEquals("Error processing request: " + errorMessage, errorResponse.getMessage());
        assertNull(errorResponse.getData());
    }

    /**
     * Test successful retrieval of posts feed by distance
     */
    @Test
    void getPostsFeedByDistance_Success() {
        // Arrange
        Double lat = 20.0;
        Double lon = 10.0;
        int page = 0;
        int size = 10;
        Pageable pageable = PageRequest.of(page, size);
        
        when(postService.findPostsByDistanceFeed(lat, lon, pageable))
                .thenReturn(mockPage);
                
        // Act
        ResponseEntity<PostResponse> response = postController.getPostsFeedByDistance(lat, lon, page, size);
        
        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        PostResponse postResponse = response.getBody();
        assertTrue(postResponse.isSuccess());
        assertNotNull(postResponse.getData());
        verify(postService).findPostsByDistanceFeed(lat, lon, pageable);
    }
    
    /**
     * Test missing latitude parameter
     */
    @Test
    void getPostsFeedByDistance_MissingLatitude() {
        // Act
        ResponseEntity<PostResponse> response = postController.getPostsFeedByDistance(null, 10.0, 0, 10);
        
        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        PostResponse errorResponse = response.getBody();
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
        ResponseEntity<PostResponse> response = postController.getPostsFeedByDistance(20.0, null, 0, 10);
        
        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        PostResponse errorResponse = response.getBody();
        assertFalse(errorResponse.isSuccess());
        assertEquals("Latitude and longitude are required", errorResponse.getMessage());
        verifyNoInteractions(postService);
    }
    
    /**
     * Test service throwing InvalidPostDataException
     */
    @Test
    void getPostsFeedByDistance_InvalidPostDataException() {
        // Arrange
        Double lat = 20.0;
        Double lon = 10.0;
        when(postService.findPostsByDistanceFeed(anyDouble(), anyDouble(), any(Pageable.class)))
                .thenThrow(new InvalidPostDataException("Invalid post data"));
                
        // Act
        ResponseEntity<PostResponse> response = postController.getPostsFeedByDistance(lat, lon, 0, 10);
        
        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        PostResponse errorResponse = response.getBody();
        assertFalse(errorResponse.isSuccess());
        assertEquals("Invalid post data", errorResponse.getMessage());
    }
    
    /**
     * Test service throwing generic Exception
     */
    @Test
    void getPostsFeedByDistance_GenericException() {
        // Arrange
        Double lat = 20.0;
        Double lon = 10.0;
        String errorMessage = "Database error";
        when(postService.findPostsByDistanceFeed(anyDouble(), anyDouble(), any(Pageable.class)))
                .thenThrow(new RuntimeException(errorMessage));
                
        // Act
        ResponseEntity<PostResponse> response = postController.getPostsFeedByDistance(lat, lon, 0, 10);
        
        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        PostResponse errorResponse = response.getBody();
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
        
        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        PostResponse postResponse = response.getBody();
        assertTrue(postResponse.isSuccess());
        assertNotNull(postResponse.getData());
        verify(postService).findPostsByTimestampFeed(pageable);
    }
    
    /**
     * Test exception during retrieval of posts feed by timestamp
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
        
        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        PostResponse errorResponse = response.getBody();
        assertFalse(errorResponse.isSuccess());
        assertEquals("Error processing request: " + errorMessage, errorResponse.getMessage());
    }
}