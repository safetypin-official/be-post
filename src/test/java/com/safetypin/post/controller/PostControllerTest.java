package com.safetypin.post.controller;

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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostControllerTest {

    @Mock
    private PostService postService;

    @InjectMocks
    private PostController postController;

    private Page<Map<String, Object>> mockPage;

    @BeforeEach
    void setUp() {
        mockPage = new PageImpl<>(new ArrayList<>());
    }

    /**
     * Test missing latitude parameter
     */
    @Test
    void whenGetPostsWithMissingLat_thenReturnBadRequest() {
        ResponseEntity<?> response = postController.getPosts(null, 10.0, 10.0, null, null, null, 0, 10);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<String, String> errorResponse = (Map<String, String>) response.getBody();
        assertEquals("Latitude and longitude are required", errorResponse.get("message"));
    }

    /**
     * Test missing longitude parameter
     */
    @Test
    void whenGetPostsWithMissingLon_thenReturnBadRequest() {
        ResponseEntity<?> response = postController.getPosts(20.0, null, 10.0, null, null, null, 0, 10);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<String, String> errorResponse = (Map<String, String>) response.getBody();
        assertEquals("Latitude and longitude are required", errorResponse.get("message"));
    }

    /**
     * Test both lat and lon missing
     */
    @Test
    void whenGetPostsWithMissingLatAndLon_thenReturnBadRequest() {
        ResponseEntity<?> response = postController.getPosts(null, null, 10.0, null, null, null, 0, 10);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<String, String> errorResponse = (Map<String, String>) response.getBody();
        assertEquals("Latitude and longitude are required", errorResponse.get("message"));
    }

    /**
     * Test valid request with all parameters
     */
    @Test
    void whenGetPostsWithValidParams_thenReturnPosts() {
        Double lat = 20.0;
        Double lon = 10.0;
        Double radius = 10.0;
        String category = "safety";
        LocalDate dateFrom = LocalDate.now().minusDays(7);
        LocalDate dateTo = LocalDate.now();
        int page = 0;
        int size = 10;

        LocalDateTime fromDateTime = LocalDateTime.of(dateFrom, LocalTime.MIN);
        LocalDateTime toDateTime = LocalDateTime.of(dateTo, LocalTime.MAX);
        Pageable pageable = PageRequest.of(page, size);

        when(postService.findPostsByLocation(lat, lon, radius, category, fromDateTime, toDateTime, pageable))
                .thenReturn(mockPage);

        ResponseEntity<?> response = postController.getPosts(lat, lon, radius, category, dateFrom, dateTo, page, size);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(mockPage, response.getBody());
        verify(postService).findPostsByLocation(lat, lon, radius, category, fromDateTime, toDateTime, pageable);
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

        ResponseEntity<?> response = postController.getPosts(lat, lon, null, null, null, null, page, size);

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

        ResponseEntity<?> response = postController.getPosts(lat, lon, radius, null, dateFrom, null, page, size);

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

        ResponseEntity<?> response = postController.getPosts(lat, lon, radius, null, null, dateTo, page, size);

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

        ResponseEntity<?> response = postController.getPosts(lat, lon, 10.0, null, null, null, page, size);

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

        ResponseEntity<?> response = postController.getPosts(lat, lon, 10.0, null, null, null, 0, 10);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<String, String> errorResponse = (Map<String, String>) response.getBody();
        assertEquals("Invalid location parameters", errorResponse.get("message"));
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

        ResponseEntity<?> response = postController.getPosts(lat, lon, 10.0, null, null, null, 0, 10);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        Map<String, String> errorResponse = (Map<String, String>) response.getBody();
        assertEquals("Error processing request: " + errorMessage, errorResponse.get("message"));
    }

    /**
     * Test MethodArgumentTypeMismatchException handler
     */
    @Test
    void handleArgumentTypeMismatchException_returnsCorrectErrorResponse() {
        MethodArgumentTypeMismatchException ex = mock(MethodArgumentTypeMismatchException.class);
        ResponseEntity<Map<String, String>> response = postController.handleArgumentTypeMismatch(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Invalid location parameters", response.getBody().get("message"));
    }
}