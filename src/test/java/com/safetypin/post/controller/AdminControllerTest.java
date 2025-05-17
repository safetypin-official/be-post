package com.safetypin.post.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import com.safetypin.post.config.TestSecurityConfig;
import com.safetypin.post.dto.UserDetails;
import com.safetypin.post.exception.UnauthorizedAccessException;
import com.safetypin.post.model.Role;
import com.safetypin.post.service.AdminService;

@WebMvcTest(AdminController.class)
@Import(TestSecurityConfig.class)
class AdminControllerTest {

    @Autowired
    private AdminService adminService;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    private UUID validUserId;
    private UUID validModeratorId;
    private UserDetails moderatorDetails;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        validUserId = UUID.randomUUID();
        validModeratorId = UUID.randomUUID();

        // Set up UserDetails for a moderator
        moderatorDetails = new UserDetails(
                Role.MODERATOR,
                true,
                validModeratorId,
                "Moderator User");

        // Setup security context
        when(authentication.getPrincipal()).thenReturn(moderatorDetails);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        // Setup service response
        when(adminService.deleteAllUserContent(any(), any(), any()))
                .thenReturn(CompletableFuture.completedFuture(null));
    }

    @Test
    void deleteUserContent_WithModeratorRole_ReturnsAccepted() {
        // Create controller instance with mocked service
        AdminController controller = new AdminController(adminService);

        // Set up SecurityContext with moderator role
        SecurityContextHolder.setContext(securityContext);

        // Call the controller method directly
        ResponseEntity<Map<String, String>> response = controller.deleteUserContent(validUserId);

        // Verify response status and body
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());

        assertEquals(validUserId.toString(), response.getBody().get("userId"));

        // Verify service was called with correct parameters
        verify(adminService, times(1)).deleteAllUserContent(
                validUserId,
                validModeratorId,
                Role.MODERATOR);
    }

    @Test
    void deleteUserContent_WithRegularUserRole_ThrowsUnauthorizedException() {
        // Setup UserDetails for a regular user
        UserDetails regularUserDetails = new UserDetails(
                Role.REGISTERED_USER,
                true,
                validModeratorId,
                "Regular User");

        // Setup authentication with regular user details
        when(authentication.getPrincipal()).thenReturn(regularUserDetails);

        // Create controller instance for direct testing
        AdminController controller = new AdminController(adminService);

        // Assert that the controller throws an exception for non-moderators
        assertThrows(UnauthorizedAccessException.class, () -> controller.deleteUserContent(validUserId));

        // Verify that the service was never called
        verify(adminService, never()).deleteAllUserContent(any(), any(), any());
    }

    @Test
    void deleteUserContent_WithNullAuthentication_ThrowsUnauthorizedException() {
        // Setup null authentication
        when(securityContext.getAuthentication()).thenReturn(null);

        // Create controller instance for direct testing
        AdminController controller = new AdminController(adminService);

        // Assert that the controller throws an exception for null authentication
        assertThrows(UnauthorizedAccessException.class, () -> controller.deleteUserContent(validUserId));

        // Verify that the service was never called
        verify(adminService, never()).deleteAllUserContent(any(), any(), any());
    }
}
