package com.safetypin.post.controller;

import com.safetypin.post.dto.PostResponse;
import com.safetypin.post.dto.UserDetails;
import com.safetypin.post.service.VoteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Objects;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VoteControllerTest {

    @Mock
    private Authentication authentication;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private VoteService voteService;

    @InjectMocks
    private VoteController voteController;

    private UUID postId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        postId = UUID.randomUUID();

        // Setup Security Context
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        // Setup User
        userId = UUID.randomUUID();
        UserDetails userDetails = mock(UserDetails.class);
        when(userDetails.getUserId()).thenReturn(userId);
        when(authentication.getPrincipal()).thenReturn(userDetails);
    }

    @Test
    void testVoteEndpoint_Upvote_Success() {
        // Given
        String expectedMessage = "Vote recorded successfully";

        when(voteService.createVote(userId, postId, true)).thenReturn(expectedMessage);

        // When
        ResponseEntity<PostResponse> response = voteController.upvote(postId);

        // Then
        assertEquals(200, response.getStatusCode().value());
        assertEquals(expectedMessage, Objects.requireNonNull(response.getBody()).getMessage());
        verify(voteService, times(1)).createVote(userId, postId, true);
    }

    @Test
    void testVoteEndpoint_Downvote_Success() {
        // Given
        boolean isUpvote = false;
        String expectedMessage = "Vote recorded successfully";

        when(voteService.createVote(userId, postId, isUpvote)).thenReturn(expectedMessage);

        // When
        ResponseEntity<PostResponse> response = voteController.downvote(postId);

        // Then
        assertEquals(200, response.getStatusCode().value());
        assertEquals(expectedMessage, Objects.requireNonNull(response.getBody()).getMessage());
        verify(voteService, times(1)).createVote(userId, postId, isUpvote);
    }

    @Test
    void testUpvoteEndpoint_AlreadyVoted_NoChange() {
        // Given
        String expectedMessage = "User already up voted that post. Vote remains unchanged";

        when(voteService.createVote(userId, postId, true)).thenReturn(expectedMessage);

        // When
        ResponseEntity<PostResponse> response = voteController.upvote( postId);

        // Then
        assertEquals(200, response.getStatusCode().value());
        assertEquals(expectedMessage, Objects.requireNonNull(response.getBody()).getMessage());
        verify(voteService, times(1)).createVote(userId, postId, true);
    }

    @Test
    void testCancelVoteEndpoint_Success() {
        // Given
        String expectedMessage = "Vote cancelled successfully";
        when(voteService.cancelVote(userId, postId)).thenReturn(expectedMessage);

        // When
        ResponseEntity<PostResponse> response = voteController.cancelVote(postId);

        // Then
        assertEquals(200, response.getStatusCode().value());
        assertEquals(expectedMessage, Objects.requireNonNull(response.getBody()).getMessage());
        verify(voteService, times(1)).cancelVote(userId, postId);
    }

    @Test
    void testCancelVoteEndpoint_NoVoteExists_NoChange() {
        // Given
        String expectedMessage = "User hasn't voted that post. Vote remains unchanged";
        when(voteService.cancelVote(userId, postId)).thenReturn(expectedMessage);

        // When
        ResponseEntity<PostResponse> response = voteController.cancelVote(postId);

        // Then
        assertEquals(200, response.getStatusCode().value());
        assertEquals(expectedMessage, Objects.requireNonNull(response.getBody()).getMessage());
        verify(voteService, times(1)).cancelVote(userId, postId);
    }


}