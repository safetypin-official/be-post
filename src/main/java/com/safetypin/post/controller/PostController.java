package com.safetypin.post.controller;

import com.safetypin.post.dto.*;
import com.safetypin.post.exception.InvalidPostDataException;
import com.safetypin.post.exception.PostNotFoundException;
import com.safetypin.post.exception.UnauthorizedAccessException;
import com.safetypin.post.model.Post;
import com.safetypin.post.service.PostService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
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
            @RequestParam(defaultValue = "30") int size) {
        return executeWithExceptionHandling(() -> {
            // Get user details from security context
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            UUID userId = userDetails.getUserId();

            Pageable pageable = PageRequest.of(page, size);
            Page<Post> postsPage = postService.findAllPaginated(pageable);

            // fetch profiles
            Map<UUID, PostedByData> profileList = postService.fetchPostedByData(postsPage.getContent().stream().map(
                    Post::getPostedBy).toList());

            List<PostData> formattedPosts = postsPage.getContent().stream()
                    .map(post -> PostData.fromPostAndUserId(post, userId, profileList.get(post.getPostedBy())))
                    .toList();

            Map<String, Object> paginationData = createPaginationData(
                    new PageImpl<>(formattedPosts, pageable,
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

            // Create request DTO
            FeedRequestDTO requestDTO = FeedRequestDTO.builder()
                    .categories(categories)
                    .keyword(keyword)
                    .dateFrom(dateFrom)
                    .dateTo(dateTo)
                    .page(page)
                    .size(size)
                    .lat(lat)
                    .lon(lon)
                    .build();

            // Convert to FeedQueryDTO
            FeedQueryDTO queryDTO = FeedQueryDTO.fromFeedRequestAndUserId(requestDTO, userId);

            // Get posts using strategy pattern
            Page<Map<String, Object>> posts = postService.getFeed(queryDTO, "distance");

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

            // Create request DTO
            FeedRequestDTO requestDTO = FeedRequestDTO.builder()
                    .categories(categories)
                    .keyword(keyword)
                    .dateFrom(dateFrom)
                    .dateTo(dateTo)
                    .page(page)
                    .size(size)
                    .build();

            // Convert to FeedQueryDTO
            FeedQueryDTO queryDTO = FeedQueryDTO.fromFeedRequestAndUserId(requestDTO, userId);

            // Get posts using strategy pattern
            Page<Map<String, Object>> posts = postService.getFeed(queryDTO, "timestamp");

            // Create response with pagination data
            Map<String, Object> paginationData = createPaginationData(posts);

            return createSuccessResponse(paginationData);
        }, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @GetMapping("/feed/following") // New endpoint
    public ResponseEntity<PostResponse> getPostsFeedByFollowing(
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

            // Create request DTO (No lat/lon needed for following feed)
            FeedRequestDTO requestDTO = FeedRequestDTO.builder()
                    .categories(categories)
                    .keyword(keyword)
                    .dateFrom(dateFrom)
                    .dateTo(dateTo)
                    .page(page)
                    .size(size)
                    // lat and lon are not set here
                    .build();

            // Convert to FeedQueryDTO
            FeedQueryDTO queryDTO = FeedQueryDTO.fromFeedRequestAndUserId(requestDTO, userId);

            // Get posts using strategy pattern
            Page<Map<String, Object>> posts = postService.getFeed(queryDTO, "following"); // Use "following" type

            // Create response with pagination data
            Map<String, Object> paginationData = createPaginationData(posts);

            return createSuccessResponse(paginationData);
        }, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @GetMapping("/user")
    public ResponseEntity<PostResponse> getPostBySpecificUser(
            @RequestParam UUID postUserId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return executeWithExceptionHandling(() -> {
            if (postUserId == null) {
                return createErrorResponse(HttpStatus.BAD_REQUEST, "User ID is required");
            }

            // Set up pagination
            Pageable pageable = PageRequest.of(page, size);

            // Get posts sorted by timestamp with filters
            Page<Map<String, Object>> posts;
            posts = postService.findPostsByUser(postUserId, pageable);

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

            request.setPostedBy(userId); // Set the postedBy field
            // Create the post
            Post post = postService.createPost(request);

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
            // Get userId from security context
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            UUID userId = userDetails.getUserId();

            Post post = postService.findById(id);

            // fetch profiles
            Map<UUID, PostedByData> profileList = postService.fetchPostedByData(
                    List.of(post.getPostedBy()));

            PostData postData = PostData.fromPostAndUserId(post, userId, profileList.get(post.getPostedBy()));
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
