package com.safetypin.post.controller;

import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.safetypin.post.exception.UnauthorizedAccessException;
import com.safetypin.post.model.Role;
import com.safetypin.post.service.AdminService;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/posts/admin")
@AllArgsConstructor
public class AdminController {

    private final AdminService adminService;

    /**
     * Endpoint to delete all content created by a specific user.
     * This includes posts, comments, and votes.
     * This operation is restricted to users with the MODERATOR role only.
     *
     * @param userId The ID of the user whose content should be deleted
     * @return A response indicating the status of the deletion operation
     */
    @DeleteMapping("/delete/{userId}")
    public ResponseEntity<Map<String, String>> deleteUserContent(@PathVariable("userId") UUID userId) {
        // Get the authentication details from the security context
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null) {
            log.error("Authentication context is null");
            throw new UnauthorizedAccessException("Unauthorized access");
        } // Extract user details from the authentication principal
        com.safetypin.post.dto.UserDetails userDetails = (com.safetypin.post.dto.UserDetails) authentication
                .getPrincipal();

        // Get user ID and role
        UUID moderatorId = userDetails.getUserId();
        Role moderatorRole = userDetails.getRole(); // Verify that the user has MODERATOR role
        if (moderatorRole != Role.MODERATOR) {
            log.error("User {} attempted to access restricted admin endpoint with role {}", moderatorId, moderatorRole);
            throw new UnauthorizedAccessException("Only moderators can access this endpoint");
        }

        log.info("Moderator {} requested deletion of all content for user {}", moderatorId, userId);

        // Delegate the deletion to the service layer as an asynchronous operation
        adminService.deleteAllUserContent(userId, moderatorId, moderatorRole);

        // Return immediately while the deletion happens in the background
        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(Map.of("message", "User content deletion initiated successfully",
                        "userId", userId.toString()));
    }
}
