package com.safetypin.post.controller;

import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.safetypin.post.dto.CommentRequest;
import com.safetypin.post.dto.PostResponse;
import com.safetypin.post.dto.UserDetails;
import com.safetypin.post.exception.InvalidPostDataException;
import com.safetypin.post.exception.PostNotFoundException;
import com.safetypin.post.exception.UnauthorizedAccessException;
import com.safetypin.post.model.CommentOnComment;
import com.safetypin.post.model.CommentOnPost;
import com.safetypin.post.service.CommentService;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/comment")
@AllArgsConstructor
public class CommentController {

    private final CommentService commentService;

    // create commentOnPost
    @PostMapping("/onpost")
    public ResponseEntity<PostResponse> createCommentOnPost(@RequestBody CommentRequest req) {

        return executeWithExceptionHandling(() -> {
            // Get user details from security context
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            UUID userId = userDetails.getUserId();

            // Create the comment
            CommentOnPost comment = commentService.createCommentOnPost(userId, req);

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
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            UUID userId = userDetails.getUserId();

            // Create the comment
            CommentOnComment comment = commentService.createCommentOnComment(userId, req);

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
}
