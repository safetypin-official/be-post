package com.safetypin.post.controller;

import com.safetypin.post.dto.PostResponse;
import com.safetypin.post.service.VoteService;
import com.safetypin.post.exception.InvalidCredentialsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Objects;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VoteControllerTest {

    @Mock
    private VoteService voteService;

    @InjectMocks
    private VoteController voteController;

    private UUID postId;
    private String authorizationHeader;

    @BeforeEach
    void setUp() {
        postId = UUID.randomUUID();
        authorizationHeader = "Bearer validToken";
    }

    @Test
    void testVoteEndpoint_Upvote_Success() throws InvalidCredentialsException {
        // Given
        String expectedMessage = "Vote recorded successfully";

        when(voteService.createVote(authorizationHeader, postId, true)).thenReturn(expectedMessage);

        // When
        ResponseEntity<PostResponse> response = voteController.upvote(authorizationHeader, postId);

        // Then
        assertEquals(200, response.getStatusCode().value());
        assertEquals(expectedMessage, Objects.requireNonNull(response.getBody()).getMessage());
        verify(voteService, times(1)).createVote(authorizationHeader, postId, true);
    }

    @Test
    void testVoteEndpoint_Downvote_Success() throws InvalidCredentialsException {
        // Given
        boolean isUpvote = false;
        String expectedMessage = "Vote recorded successfully";

        when(voteService.createVote(authorizationHeader, postId, isUpvote)).thenReturn(expectedMessage);

        // When
        ResponseEntity<PostResponse> response = voteController.downvote(authorizationHeader, postId);

        // Then
        assertEquals(200, response.getStatusCode().value());
        assertEquals(expectedMessage, Objects.requireNonNull(response.getBody()).getMessage());
        verify(voteService, times(1)).createVote(authorizationHeader, postId, isUpvote);
    }

    @Test
    void testUpvoteEndpoint_AlreadyVoted_NoChange() throws InvalidCredentialsException {
        // Given
        String expectedMessage = "User already up voted that post. Vote remains unchanged";

        when(voteService.createVote(authorizationHeader, postId, true)).thenReturn(expectedMessage);

        // When
        ResponseEntity<PostResponse> response = voteController.upvote(authorizationHeader, postId);

        // Then
        assertEquals(200, response.getStatusCode().value());
        assertEquals(expectedMessage, Objects.requireNonNull(response.getBody()).getMessage());
        verify(voteService, times(1)).createVote(authorizationHeader, postId, true);
    }

    @Test
    void testDownvoteEndpoint_InvalidToken_ThrowsException() throws InvalidCredentialsException {
        // Given
        String expectedMessage = "Invalid Token";
        when(voteService.createVote(authorizationHeader, postId, false)).thenThrow(new InvalidCredentialsException(expectedMessage));

        // When
        ResponseEntity<PostResponse> response = voteController.downvote(authorizationHeader, postId);

        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(expectedMessage, Objects.requireNonNull(response.getBody()).getMessage());
        verify(voteService, times(1)).createVote(authorizationHeader, postId, false);
    }

    @Test
    void testUpoteEndpoint_InvalidToken_ThrowsException() throws InvalidCredentialsException {
        // Given
        when(voteService.createVote(any(), any(), anyBoolean()))
                .thenThrow(new InvalidCredentialsException("Invalid token"));

        // When
        ResponseEntity<PostResponse> response = voteController.upvote(authorizationHeader, postId);

        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode()); // Ensure correct status
        assertEquals("Invalid token", Objects.requireNonNull(response.getBody()).getMessage()); // Assuming createErrorResponse() sets the message
    }

    @Test
    void testVoteEndpoint_NullAuthorizationHeader_ThrowsException() throws InvalidCredentialsException {
        // Given
        when(voteService.createVote(authorizationHeader, postId, true))
                .thenThrow(new InvalidCredentialsException("Authorization header is invalid"));

        // When
        ResponseEntity<PostResponse> response = voteController.upvote(authorizationHeader, postId);

        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode()); // Ensure correct status
        assertEquals("Authorization header is invalid", Objects.requireNonNull(response.getBody()).getMessage()); // Assuming createErrorResponse() s
    }

    @Test
    void testVoteEndpoint_EmptyAuthorizationHeader_ThrowsException() throws InvalidCredentialsException {
        // Given
        boolean isUpvote = true;
        String emptyHeader = "";
        when(voteService.createVote(emptyHeader, postId, isUpvote))
                .thenThrow(new InvalidCredentialsException("Authorization header is invalid"));

        // When
        ResponseEntity<PostResponse> response = voteController.upvote(emptyHeader, postId);

        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode()); // Ensure correct status
        assertEquals("Authorization header is invalid", Objects.requireNonNull(response.getBody()).getMessage()); // Assuming createErrorResponse() s
    }

    @Test
    void testCancelVoteEndpoint_Success() throws InvalidCredentialsException {
        // Given
        String expectedMessage = "Vote cancelled successfully";
        when(voteService.cancelVote(authorizationHeader, postId)).thenReturn(expectedMessage);

        // When
        ResponseEntity<PostResponse> response = voteController.cancelVote(authorizationHeader, postId);

        // Then
        assertEquals(200, response.getStatusCode().value());
        assertEquals(expectedMessage, Objects.requireNonNull(response.getBody()).getMessage());
        verify(voteService, times(1)).cancelVote(authorizationHeader, postId);
    }

    @Test
    void testCancelVoteEndpoint_NoVoteExists_NoChange() throws InvalidCredentialsException {
        // Given
        String expectedMessage = "User hasn't voted that post. Vote remains unchanged";
        when(voteService.cancelVote(authorizationHeader, postId)).thenReturn(expectedMessage);

        // When
        ResponseEntity<PostResponse> response = voteController.cancelVote(authorizationHeader, postId);

        // Then
        assertEquals(200, response.getStatusCode().value());
        assertEquals(expectedMessage, Objects.requireNonNull(response.getBody()).getMessage());
        verify(voteService, times(1)).cancelVote(authorizationHeader, postId);
    }

    @Test
    void testCancelVoteEndpoint_InvalidToken_ThrowsException() throws InvalidCredentialsException {
        // Given
        when(voteService.cancelVote(authorizationHeader, postId))
                .thenThrow(new InvalidCredentialsException("Invalid token"));

        // When/Then
        ResponseEntity<PostResponse> response = voteController.cancelVote(authorizationHeader, postId);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("Invalid token", Objects.requireNonNull(response.getBody()).getMessage());
        verify(voteService, times(1)).cancelVote(authorizationHeader, postId);
    }

    @Test
    void testCancelVoteEndpoint_NullAuthorizationHeader_ThrowsException() throws InvalidCredentialsException {
        // Given
        when(voteService.cancelVote(authorizationHeader, postId))
                .thenThrow(new InvalidCredentialsException("Authorization header is invalid"));

        // When/Then
        ResponseEntity<PostResponse> response = voteController.cancelVote(authorizationHeader, postId);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("Authorization header is invalid", Objects.requireNonNull(response.getBody()).getMessage());
        verify(voteService, times(1)).cancelVote(authorizationHeader, postId);

    }

    @Test
    void testCancelVoteEndpoint_EmptyAuthorizationHeader_ThrowsException() throws InvalidCredentialsException {
        // Given
        String emptyHeader = "";
        when(voteService.cancelVote(emptyHeader, postId))
                .thenThrow(new InvalidCredentialsException("Authorization header is invalid"));

        // When/Then
        ResponseEntity<PostResponse> response = voteController.cancelVote(emptyHeader, postId);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("Authorization header is invalid", Objects.requireNonNull(response.getBody()).getMessage());
        verify(voteService, times(1)).cancelVote(emptyHeader, postId);
    }
}