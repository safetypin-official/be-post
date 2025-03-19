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
import org.mockito.Mockito;
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
    private Category mockCategory;
    private Post mockPost;
    private PostCreateRequest validRequest;
    //private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        //objectMapper = new ObjectMapper();
        mockPage = new PageImpl<>(new ArrayList<>());

        mockCategory = new Category();
        mockCategory.setId(UUID.randomUUID());
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

    @Test
    void testFindAllPosts() {
        // Mock Data
        Post post1 = new Post();
        post1.setId(UUID.randomUUID());
        post1.setCaption("First Post");
        post1.setTitle("Title 1");

        Post post2 = new Post();
        post2.setId(UUID.randomUUID());
        post2.setCaption("Second Post");
        post2.setTitle("Title 2");

        List<Post> mockPosts = Arrays.asList(post1, post2);

        // Mock Service Call
        Mockito.when(postService.findAll()).thenReturn(mockPosts);

        // Call Controller Method
        List<Post> result = postController.findAll();

        // Assertions
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("First Post", result.get(0).getCaption());
        assertEquals("Second Post", result.get(1).getCaption());

        // Verify interaction with mock
        Mockito.verify(postService).findAll();
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
     * verify(postService).findPostsByLocation(lat, lon, radius, null, null, toDateTime, pageable);
     * }
     * <p>
     * /**
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
        assertEquals("Error creating post: Unexpected runtime exception", response.getBody().getMessage());
        assertNull(response.getBody().getData());
    }
}