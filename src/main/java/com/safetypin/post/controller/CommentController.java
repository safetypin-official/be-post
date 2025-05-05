package com.safetypin.post.controller;

import com.safetypin.post.dto.*;
import com.safetypin.post.exception.InvalidPostDataException;
import com.safetypin.post.exception.PostNotFoundException;
import com.safetypin.post.exception.UnauthorizedAccessException;
import com.safetypin.post.model.CommentOnComment;
import com.safetypin.post.model.CommentOnPost;
import com.safetypin.post.service.CommentService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

@Slf4j
@RestController
@RequestMapping("/posts/comment")
@AllArgsConstructor
public class CommentController {

    private final CommentService commentService;


    @GetMapping("/postedby/{userId}")
    public ResponseEntity<PostResponse> getCommentsPostedBy(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return executeWithExceptionHandling(() -> {

            // make pageable
            Pageable pageable = PageRequest.of(page, size);

            // Get the comment
            Page<CommentDTOWithPostId> comments = commentService.getCommentsByPostedBy(userId, pageable);

            // Create response with pagination data
            Map<String, Object> paginationData = createPaginationData(comments);

            // Return success response
            return createSuccessResponse(paginationData);
        }, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // fetch comments on post
    @GetMapping("/onpost/{postId}")
    public ResponseEntity<PostResponse> getCommentOnPost(
            @PathVariable UUID postId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return executeWithExceptionHandling(() -> {

            // make pageable
            Pageable pageable = PageRequest.of(page, size);

            // Get the comment
            Page<CommentDTO> comments = commentService.getCommentOnPost(postId, pageable);

            // Create response with pagination data
            Map<String, Object> paginationData = createPaginationData(comments);

            // Return success response
            return createSuccessResponse(paginationData);
        }, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // fetch comments on post
    @GetMapping("/oncomment/{commentId}")
    public ResponseEntity<PostResponse> getCommentOnComment(
            @PathVariable UUID commentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return executeWithExceptionHandling(() -> {

            // userId is not required to fetch comments

            // make pageable
            Pageable pageable = PageRequest.of(page, size);

            // Get the comment
            Page<CommentDTO> comments = commentService.getCommentOnComment(commentId, pageable);

            // Create response with pagination data
            Map<String, Object> paginationData = createPaginationData(comments);

            // Return success response
            return createSuccessResponse(paginationData);
        }, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // create commentOnPost
    @PostMapping("/onpost")
    public ResponseEntity<PostResponse> createCommentOnPost(@RequestBody CommentRequest req) {

        return executeWithExceptionHandling(() -> {
            SecurityContext context = SecurityContextHolder.getContext();
            if (context == null || context.getAuthentication() == null) {
                return ResponseEntity
                        .status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(new PostResponse(false, "Error processing request: Security context not available", null));
            }

            // Get user details from security context
            Authentication authentication = context.getAuthentication();

            // Create the comment
            CommentOnPost comment = commentService.createCommentOnPost(req);

            // Return success response
            PostResponse response = new PostResponse(
                    true,
                    "Comment created successfully",
                    comment);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(response);
        }, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // create commentOnComment
    @PostMapping("/oncomment")
    public ResponseEntity<PostResponse> createCommentOnComment(@RequestBody CommentRequest req) {

        return executeWithExceptionHandling(() -> {
            // Get user details from security context
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            // Create the comment
            CommentOnComment comment = commentService.createCommentOnComment(req);

            // Return success response
            PostResponse response = new PostResponse(
                    true,
                    "Comment created successfully",
                    comment);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(response);
        }, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // delete a parent comment (CommentOnPost)
    @DeleteMapping("/onpost/{commentId}")
    public ResponseEntity<PostResponse> deleteCommentOnPost(@PathVariable UUID commentId) {
        log.info("Received request to delete parent comment with ID: {}", commentId);

        return executeWithExceptionHandling(() -> {
            // Get user details from security context
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            UUID userId = userDetails.getUserId();

            // Delete the parent comment (and all its child comments)
            commentService.deleteComment(commentId, userId, false);

            // Return success response
            Map<String, Object> responseData = Map.of(
                    "commentId", commentId,
                    "deletedBy", userId,
                    "type", "parent comment");

            PostResponse response = new PostResponse(
                    true,
                    "Comment and all its replies deleted successfully",
                    responseData);

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(response);
        }, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // delete a child comment (CommentOnComment)
    @DeleteMapping("/oncomment/{commentId}")
    public ResponseEntity<PostResponse> deleteCommentOnComment(@PathVariable UUID commentId) {
        log.info("Received request to delete child comment with ID: {}", commentId);

        return executeWithExceptionHandling(() -> {
            // Get user details from security context
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            UUID userId = userDetails.getUserId();

            // Delete the child comment
            commentService.deleteComment(commentId, userId, true);

            // Return success response
            Map<String, Object> responseData = Map.of(
                    "commentId", commentId,
                    "deletedBy", userId,
                    "type", "child comment");

            PostResponse response = new PostResponse(
                    true,
                    "Reply deleted successfully",
                    responseData);

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(response);
        }, HttpStatus.INTERNAL_SERVER_ERROR);
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
}
