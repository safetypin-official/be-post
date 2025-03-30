package com.safetypin.post.controller;

import com.safetypin.post.dto.PostCreateRequest;
import com.safetypin.post.dto.PostResponse;
import com.safetypin.post.dto.UserDetails;
import com.safetypin.post.exception.InvalidPostDataException;
import com.safetypin.post.exception.PostNotFoundException;
import com.safetypin.post.exception.UnauthorizedAccessException;
import com.safetypin.post.model.Post;
import com.safetypin.post.service.PostService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

@Slf4j
@RestController
@RequestMapping("/posts")
public class PostController {
    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    // Helper method to format post data
    private Map<String, Object> formatPostData(Post post) {
        Map<String, Object> postData = new HashMap<>();
        postData.put("id", post.getId());
        postData.put("title", post.getTitle());
        postData.put("caption", post.getCaption());
        postData.put("latitude", post.getLatitude());
        postData.put("longitude", post.getLongitude());
        postData.put("createdAt", post.getCreatedAt());
        postData.put("category", post.getCategory());
        postData.put("postedBy", post.getPostedBy()); // Add postedBy to response
        return postData;
    }

    // Helper method to create pagination data from a Page object
    private <T> Map<String, Object> createPaginationData(Page<T> page) {
        return Map.of(
                "content", page.getContent(),
                "totalPages", page.getTotalPages(),
                "totalElements", page.getTotalElements(),
                "currentPage", page.getNumber(),
                "pageSize", page.getSize(),
                "hasNext", page.hasNext(),
                "hasPrevious", page.hasPrevious());
    }

    // Helper method to create a pageable object
    private Pageable createPageable(int page, int size) {
        return PageRequest.of(page, size);
    }

    // Generic exception handler for controller methods
    private ResponseEntity<PostResponse> executeWithExceptionHandling(
            Supplier<ResponseEntity<PostResponse>> action,
            HttpStatus errorStatus) {
        try {
            return action.get();
        } catch (InvalidPostDataException e) {
            return createErrorResponse(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (PostNotFoundException e) {
            return createErrorResponse(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (UnauthorizedAccessException e) {
            return createErrorResponse(HttpStatus.FORBIDDEN, e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error occurred: {}", e.getMessage(), e);
            return createErrorResponse(errorStatus, "Error processing request: " + e.getMessage());
        }
    }

    // Helper method to create error responses
    private ResponseEntity<PostResponse> createErrorResponse(HttpStatus status, String message) {
        PostResponse errorResponse = new PostResponse(false, message, null);
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(errorResponse);
    }

    // Helper method to create success responses
    private ResponseEntity<PostResponse> createSuccessResponse(Object data) {
        PostResponse response = new PostResponse(true, null, data);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
    }

    // Helper method to validate location parameters
    private void validateLocationParams(Double lat, Double lon) {
        if (lat == null || lon == null) {
            throw new InvalidPostDataException("Latitude and longitude are required");
        }
    }

    @GetMapping("/all")
    public ResponseEntity<PostResponse> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return executeWithExceptionHandling(() -> {
            // Get user details from security context
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            UUID userId = userDetails.getUserId();

            Pageable pageable = createPageable(page, size);
            Page<Post> postsPage = postService.findAllPaginated(userId, pageable);

            List<Map<String, Object>> formattedPosts = postsPage.getContent().stream()
                    .map(this::formatPostData)
                    .toList();

            Map<String, Object> paginationData = createPaginationData(
                    new org.springframework.data.domain.PageImpl<>(formattedPosts, pageable,
                            postsPage.getTotalElements()));

            return createSuccessResponse(paginationData);
        }, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @GetMapping("/feed/distance")
    public ResponseEntity<PostResponse> getPostsFeedByDistance(
            @RequestParam Double lat,
            @RequestParam Double lon,
            @RequestParam(required = false) List<String> categories,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return executeWithExceptionHandling(() -> {
            // Get user details from security context
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            UUID userId = userDetails.getUserId();

            // Validate location parameters
            validateLocationParams(lat, lon);

            // Convert LocalDate to LocalDateTime if provided
            LocalDateTime fromDateTime = dateFrom != null ? LocalDateTime.of(dateFrom, LocalTime.MIN) : null;
            LocalDateTime toDateTime = dateTo != null ? LocalDateTime.of(dateTo, LocalTime.MAX) : null;

            // Set up pagination
            Pageable pageable = createPageable(page, size);

            // Get posts sorted by distance with filters
            Page<Map<String, Object>> posts;
            posts = postService.findPostsByDistanceFeed(
                    lat, lon, categories, keyword, fromDateTime, toDateTime,
                    userId, pageable);

            // Create response with pagination data
            Map<String, Object> paginationData = createPaginationData(posts);

            return createSuccessResponse(paginationData);
        }, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @GetMapping("/feed/timestamp")
    public ResponseEntity<PostResponse> getPostsFeedByTimestamp(
            @RequestParam(required = false) List<String> categories,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return executeWithExceptionHandling(() -> {
            // Get user details from security context
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            UUID userId = userDetails.getUserId();

            // Convert LocalDate to LocalDateTime if provided
            LocalDateTime fromDateTime = dateFrom != null ? LocalDateTime.of(dateFrom, LocalTime.MIN) : null;
            LocalDateTime toDateTime = dateTo != null ? LocalDateTime.of(dateTo, LocalTime.MAX) : null;

            // Set up pagination
            Pageable pageable = createPageable(page, size);

            // Get posts sorted by timestamp with filters
            Page<Map<String, Object>> posts;
            posts = postService.findPostsByTimestampFeed(
                    categories, keyword, fromDateTime, toDateTime,
                    userId, pageable);

            // Create response with pagination data
            Map<String, Object> paginationData = createPaginationData(posts);

            return createSuccessResponse(paginationData);
        }, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PostResponse> createPost(
            @RequestBody PostCreateRequest request) {

        return executeWithExceptionHandling(() -> {
            // Get user details from security context
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            UUID userId = userDetails.getUserId();

            // Create the post
            Post post = postService.createPost(
                    request.getTitle(),
                    request.getCaption(),
                    request.getLatitude(),
                    request.getLongitude(),
                    request.getCategory(),
                    userId);

            // Return success response
            PostResponse response = new PostResponse(
                    true,
                    "Post created successfully",
                    post);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(response);
        }, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PostResponse> getPostById(@PathVariable UUID id) {
        return executeWithExceptionHandling(() -> {
            Post post = postService.findById(id);
            Map<String, Object> postData = formatPostData(post);
            return createSuccessResponse(postData);
        }, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<PostResponse> deletePost(
            @PathVariable UUID id) {

        log.info("Received request to delete post with ID: {}", id);

        // Get user details from security context
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        UUID userId = userDetails.getUserId();

        return executeWithExceptionHandling(() -> {
            // Attempt to delete the post
            postService.deletePost(id, userId);

            // Return success response
            Map<String, Object> responseData = Map.of(
                    "postId", id,
                    "deletedBy", userId);

            PostResponse response = new PostResponse(
                    true,
                    "Post deleted successfully",
                    responseData);

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(response);
        }, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<PostResponse> handleArgumentTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return createErrorResponse(HttpStatus.BAD_REQUEST, "Invalid location parameters");
    }

}
