package com.safetypin.post.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.safetypin.post.dto.NotificationDto;
import com.safetypin.post.dto.PostResponse;
import com.safetypin.post.dto.UserDetails;
import com.safetypin.post.service.NotificationService;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/post/comment-notifications") // Updated mapping
@AllArgsConstructor
public class CommentNotificationController { // Renamed controller

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<PostResponse> getCommentNotifications() { // Renamed method for clarity
        try {
            // Get user details from security context
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            UUID userId = userDetails.getUserId();

            log.info("Fetching comment notifications for user ID: {}", userId);

            List<NotificationDto> notifications = notificationService.getNotifications(userId);

            PostResponse response = new PostResponse(true, "Comment notifications retrieved successfully",
                    notifications);
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(response);

        } catch (Exception e) {
            log.error("Error fetching comment notifications: {}", e.getMessage(), e);
            return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to retrieve comment notifications: " + e.getMessage());
        }
    }

    // Helper method to create error responses
    private ResponseEntity<PostResponse> createErrorResponse(HttpStatus status, String message) {
        PostResponse errorResponse = new PostResponse(false, message, null);
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(errorResponse);
    }
}
