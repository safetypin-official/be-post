package com.safetypin.post.controller;

import com.safetypin.post.dto.PostCreateRequest;
import com.safetypin.post.dto.PostResponse;
import com.safetypin.post.exception.InvalidPostDataException;
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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/posts")
public class PostController {

    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @GetMapping("/all")
    public ResponseEntity<PostResponse> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<Post> postsPage = postService.findAllPaginated(pageable);
            
            List<Map<String, Object>> formattedPosts = postsPage.getContent().stream()
                .map(post -> {
                    Map<String, Object> postData = new HashMap<>();
                    postData.put("id", post.getId());
                    postData.put("title", post.getTitle());
                    postData.put("caption", post.getCaption());
                    postData.put("latitude", post.getLatitude());
                    postData.put("longitude", post.getLongitude());
                    postData.put("createdAt", post.getCreatedAt());
                    postData.put("category", post.getCategory());
                    return postData;
                })
                .collect(Collectors.toList());
            
            Map<String, Object> paginationData = Map.of(
                "content", formattedPosts,
                "totalPages", postsPage.getTotalPages(),
                "totalElements", postsPage.getTotalElements(),
                "currentPage", postsPage.getNumber(),
                "pageSize", postsPage.getSize(),
                "hasNext", postsPage.hasNext(),
                "hasPrevious", postsPage.hasPrevious()
            );
            
            PostResponse response = new PostResponse(true, null, paginationData);
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(response);
        } catch (Exception e) {
            PostResponse errorResponse = new PostResponse(
                false, "Error retrieving posts: " + e.getMessage(), null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.APPLICATION_JSON)
                .body(errorResponse);
        }
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PostResponse> getPosts(
            @RequestParam Double lat,
            @RequestParam Double lon,
            @RequestParam(required = false, defaultValue = "10.0") Double radius,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        try {
            // Explicitly handle null radius
            Double radiusToUse = radius != null ? radius : 10.0;

            // Require lat and lon; throw custom exception if not provided
            if (lat == null || lon == null) {
                throw new InvalidPostDataException("Latitude and longitude are required");
            }

            // convert LocalDate to localDateTime & handle null
            LocalDateTime fromDateTime = dateFrom != null ?
                    LocalDateTime.of(dateFrom, LocalTime.MIN) : null;
            LocalDateTime toDateTime = dateTo != null ?
                    LocalDateTime.of(dateTo, LocalTime.MAX) : null;

            // page
            Pageable pageable = PageRequest.of(page, size);

            // find posts
            Page<Map<String, Object>> posts = postService.findPostsByLocation(
                    lat, lon, radiusToUse, category, fromDateTime, toDateTime, pageable);
            
            // Create response with pagination metadata
            Map<String, Object> paginationData = Map.of(
                "content", posts.getContent(),
                "totalPages", posts.getTotalPages(),
                "totalElements", posts.getTotalElements(),
                "currentPage", posts.getNumber(),
                "pageSize", posts.getSize(),
                "hasNext", posts.hasNext(),
                "hasPrevious", posts.hasPrevious()
            );

            PostResponse postResponse = new PostResponse(true, null, paginationData);

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(postResponse);
        } catch (InvalidPostDataException e) {
            PostResponse errorResponse = new PostResponse(
                    false, e.getMessage(), null);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(errorResponse);
        } catch (NumberFormatException e) {
            PostResponse errorResponse = new PostResponse(
                    false, "Invalid location parameters", null);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(errorResponse);
        } catch (Exception e) {
            PostResponse errorResponse = new PostResponse(
                    false, "Error processing request: " + e.getMessage(), null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(errorResponse);
        }
    }

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PostResponse> createPost(@RequestBody PostCreateRequest request) {
        try {
            Post post = postService.createPost(
                    request.getTitle(),
                    request.getCaption(),
                    request.getLatitude(),
                    request.getLongitude(),
                    request.getCategory()
            );

            PostResponse response = new PostResponse(
                    true,
                    "Post created successfully",
                    post
            );

            return ResponseEntity.status(HttpStatus.CREATED)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(response);
        } catch (InvalidPostDataException e) {
            PostResponse errorResponse = new PostResponse(
                    false,
                    e.getMessage(),
                    null
            );

            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(errorResponse);
        } catch (Exception e) {
            PostResponse errorResponse = new PostResponse(
                    false,
                    "Error creating post: " + e.getMessage(),
                    null
            );

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(errorResponse);
        }
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<PostResponse> handleArgumentTypeMismatch(MethodArgumentTypeMismatchException ex) {
        PostResponse errorResponse = new PostResponse(
                false, "Invalid location parameters", null);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_JSON)
                .body(errorResponse);
    }
}