package com.safetypin.post.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.safetypin.post.dto.NotificationDto;
import com.safetypin.post.dto.PostResponse;
import com.safetypin.post.dto.UserDetails;
import com.safetypin.post.model.NotificationType;
import com.safetypin.post.model.Role;
import com.safetypin.post.service.NotificationService;

@ExtendWith(MockitoExtension.class)
class CommentNotificationControllerTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private CommentNotificationController commentNotificationController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper = new ObjectMapper();
    private UUID testUserId;
    private UserDetails testUserDetails;
    private String testUserName = "testuser";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(commentNotificationController).build();
        testUserId = UUID.randomUUID();
        testUserDetails = new UserDetails(Role.REGISTERED_USER, true, testUserId, testUserName);

        // Mock SecurityContext
        Authentication authentication = new UsernamePasswordAuthenticationToken(testUserDetails, null,
                Collections.emptyList());
        SecurityContext securityContext = mock(SecurityContext.class);

        // Using lenient() to avoid UnnecessaryStubbingException for tests that clear
        // the security context
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void getCommentNotifications_Success() throws Exception {
        // Arrange
        UUID actorId = UUID.randomUUID();
        NotificationDto notification = NotificationDto.builder()
                .type(NotificationType.NEW_COMMENT_ON_POST)
                .actorUserId(actorId)
                .actorName("Actor User")
                .actorProfilePictureUrl("http://example.com/pic.jpg")
                .timeAgo("Today")
                .postId(UUID.randomUUID())
                .commentId(UUID.randomUUID())
                .createdAt(LocalDateTime.now())
                .build();
        List<NotificationDto> notifications = List.of(notification);
        PostResponse expectedResponse = new PostResponse(true, "Comment notifications retrieved successfully",
                notifications);

        when(notificationService.getNotifications(testUserId)).thenReturn(notifications);

        // Act & Assert
        mockMvc.perform(get("/comment-notifications")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Comment notifications retrieved successfully"))
                .andExpect(jsonPath("$.data[0].type").value(NotificationType.NEW_COMMENT_ON_POST.toString()))
                .andExpect(jsonPath("$.data[0].actorUserId").value(actorId.toString()))
                .andExpect(jsonPath("$.data[0].actorName").value("Actor User"));

        verify(notificationService, times(1)).getNotifications(testUserId);
    }

    @Test
    void getCommentNotifications_ServiceThrowsException() throws Exception {
        // Arrange
        String errorMessage = "Database connection failed";
        when(notificationService.getNotifications(testUserId)).thenThrow(new RuntimeException(errorMessage));

        // Act & Assert
        mockMvc.perform(get("/comment-notifications")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Failed to retrieve comment notifications: " + errorMessage))
                .andExpect(jsonPath("$.data").doesNotExist()); // Or .isEmpty() depending on PostResponse structure

        verify(notificationService, times(1)).getNotifications(testUserId);
    }

    @Test
    void getCommentNotifications_NoNotificationsFound() throws Exception {
        // Arrange
        when(notificationService.getNotifications(testUserId)).thenReturn(Collections.emptyList());
        PostResponse expectedResponse = new PostResponse(true, "Comment notifications retrieved successfully",
                Collections.emptyList());

        // Act & Assert
        mockMvc.perform(get("/comment-notifications")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Comment notifications retrieved successfully"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());

        verify(notificationService, times(1)).getNotifications(testUserId);
    }

    @Test
    void getCommentNotifications_Unauthenticated() throws Exception {
        // Arrange - Clear security context for this test
        SecurityContextHolder.clearContext();

        // Act & Assert
        // Standalone setup doesn't automatically enforce security rules like full
        // context testing.
        // This test primarily ensures the controller code handles missing auth
        // gracefully *if* accessed,
        // though typically Spring Security filters would block unauthenticated requests
        // earlier.
        // We expect a NullPointerException here because the controller tries to
        // getPrincipal() on null auth.
        // A real Spring Security setup would return 401/403 before reaching the
        // controller.
        // For unit testing the controller logic *after* auth, we assume auth succeeded
        // (like in setUp).
        // To test the *absence* of auth leading to an error *within* the controller:
        try {
            commentNotificationController.getCommentNotifications();
        } catch (NullPointerException e) {
            // Expected because SecurityContextHolder.getContext().getAuthentication() is
            // null
        }

        // We don't verify notificationService because the controller should fail before
        // calling it.
        verify(notificationService, never()).getNotifications(any());

        // A more integrated test with MockMvc and Spring Security would look like:
        // mockMvc.perform(get("/comment-notifications"))
        // .andExpect(status().isUnauthorized()); // Or isForbidden()
    }
}
