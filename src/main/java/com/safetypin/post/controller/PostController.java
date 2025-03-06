package com.safetypin.post.controller;

import com.safetypin.post.dto.PostCreateRequest;
import com.safetypin.post.dto.PostResponse;
import com.safetypin.post.exception.InvalidPostDataException;
import com.safetypin.post.model.Post;
import com.safetypin.post.service.PostService;
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

@RestController
@RequestMapping("/posts")
public class PostController {

    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
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
            Double radiusToUse = radius != null ? radius : 10.0; // Explicitly handle null radius

            // Require lat and lon; throw custom exception if not provided
            if (lat == null || lon == null) {
                throw new InvalidPostDataException("Latitude and longitude are required");
            }

            LocalDateTime fromDateTime = dateFrom != null ?
                    LocalDateTime.of(dateFrom, LocalTime.MIN) : null;
            LocalDateTime toDateTime = dateTo != null ?
                    LocalDateTime.of(dateTo, LocalTime.MAX) : null;

            Pageable pageable = PageRequest.of(page, size);

            Page<?> posts = postService.findPostsByLocation(
                    lat, lon, radiusToUse, category, fromDateTime, toDateTime, pageable);

            PostResponse postResponse = new PostResponse(
                    true, null, posts);

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
            // Validation is moved to service layer to use custom exceptions
            Post createdPost = postService.createPost(
                    request.getTitle(), 
                    request.getCaption(),
                    request.getLatitude(), 
                    request.getLongitude(), 
                    request.getCategory());
            
            PostResponse postResponse = new PostResponse(
                    true, "Post created successfully", createdPost);
            
            return ResponseEntity.status(HttpStatus.CREATED)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(postResponse);
        } catch (Exception e) {
            PostResponse errorResponse = new PostResponse(
                    false, "Error creating post: " + e.getMessage(), null);
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