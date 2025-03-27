package com.safetypin.post.service;

import com.safetypin.post.model.Post;
import com.safetypin.post.model.Vote;
import com.safetypin.post.repository.PostRepository;
import com.safetypin.post.repository.VoteRepository;
import org.apache.hc.client5.http.auth.InvalidCredentialsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VoteServiceTest {

    @Mock
    private VoteRepository voteRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private VoteServiceImpl voteService;

    private UUID userId;
    private UUID postId;
    private Post post;
    private String authorizationHeader;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        postId = UUID.randomUUID();
        post = new Post();
        authorizationHeader = "Bearer mockToken";
    }

    @Test
    void createVote_NewVote_Success() throws InvalidCredentialsException {
        when(jwtService.getUserIdFromAuthorizationHeader(anyString())).thenReturn(userId);
        when(postRepository.findById(postId)).thenReturn(Optional.of(post));
        when(voteRepository.findById(any())).thenReturn(Optional.empty());

        String result = voteService.createVote(authorizationHeader, postId, true);

        assertEquals("Vote recorded successfully", result);
        verify(voteRepository, times(1)).save(any(Vote.class));
    }

    @Test
    void createVote_AlreadyUpvoted_NoChange() throws InvalidCredentialsException {
        when(jwtService.getUserIdFromAuthorizationHeader(anyString())).thenReturn(userId);
        when(postRepository.findById(postId)).thenReturn(Optional.of(post));

        Vote existingVote = new Vote(new Vote.VoteId(userId, post), true);
        when(voteRepository.findById(any())).thenReturn(Optional.of(existingVote));

        String result = voteService.createVote(authorizationHeader, postId, true);

        assertEquals("User already up voted that post. Vote remains unchanged", result);
        verify(voteRepository, never()).save(any(Vote.class));
    }

    @Test
    void createVote_AlreadyDownvoted_NoChange() throws InvalidCredentialsException {
        when(jwtService.getUserIdFromAuthorizationHeader(anyString())).thenReturn(userId);
        when(postRepository.findById(postId)).thenReturn(Optional.of(post));

        Vote existingVote = new Vote(new Vote.VoteId(userId, post), false);
        when(voteRepository.findById(any())).thenReturn(Optional.of(existingVote));

        String result = voteService.createVote(authorizationHeader, postId, false);

        assertEquals("User already down voted that post. Vote remains unchanged", result);
        verify(voteRepository, never()).save(any(Vote.class));
    }

    @Test
    void createVote_ChangeFromUpvoteToDownvote_Success() throws InvalidCredentialsException {
        when(jwtService.getUserIdFromAuthorizationHeader(anyString())).thenReturn(userId);
        when(postRepository.findById(postId)).thenReturn(Optional.of(post));

        Vote existingVote = new Vote(new Vote.VoteId(userId, post), true);
        when(voteRepository.findById(any())).thenReturn(Optional.of(existingVote));

        String result = voteService.createVote(authorizationHeader, postId, false);

        assertEquals("Vote recorded successfully", result);
        verify(voteRepository, times(1)).save(any(Vote.class));
    }

    @Test
    void createVote_ChangeFromDownvoteToUpvote_Success() throws InvalidCredentialsException {
        when(jwtService.getUserIdFromAuthorizationHeader(anyString())).thenReturn(userId);
        when(postRepository.findById(postId)).thenReturn(Optional.of(post));

        Vote existingVote = new Vote(new Vote.VoteId(userId, post), false);
        when(voteRepository.findById(any())).thenReturn(Optional.of(existingVote));

        String result = voteService.createVote(authorizationHeader, postId, true);

        assertEquals("Vote recorded successfully", result);
        verify(voteRepository, times(1)).save(any(Vote.class));
    }

    @Test
    void cancelVote_Success() throws InvalidCredentialsException {
        when(jwtService.getUserIdFromAuthorizationHeader(anyString())).thenReturn(userId);
        when(postRepository.findById(postId)).thenReturn(Optional.of(post));

        Vote existingVote = new Vote(new Vote.VoteId(userId, post), true);
        when(voteRepository.findById(any())).thenReturn(Optional.of(existingVote));

        String result = voteService.cancelVote(authorizationHeader, postId);

        assertEquals("Vote cancelled successfully", result);
        verify(voteRepository, times(1)).deleteById(any());
    }

    @Test
    void cancelVote_NoVoteExists_NoChange() throws InvalidCredentialsException {
        when(jwtService.getUserIdFromAuthorizationHeader(anyString())).thenReturn(userId);
        when(postRepository.findById(postId)).thenReturn(Optional.of(post));
        when(voteRepository.findById(any())).thenReturn(Optional.empty());

        String result = voteService.cancelVote(authorizationHeader, postId);

        assertEquals("User hasn't voted that post. Vote remains unchanged", result);
        verify(voteRepository, never()).deleteById(any());
    }

    @Test
    void createVote_PostNotFound_ThrowsException() throws InvalidCredentialsException {
        when(jwtService.getUserIdFromAuthorizationHeader(anyString())).thenReturn(userId);
        when(postRepository.findById(postId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> voteService.createVote(authorizationHeader, postId, true));
        verify(voteRepository, never()).save(any(Vote.class));
    }

    @Test
    void cancelVote_PostNotFound_ThrowsException() throws InvalidCredentialsException {
        when(jwtService.getUserIdFromAuthorizationHeader(anyString())).thenReturn(userId);
        when(postRepository.findById(postId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> voteService.cancelVote(authorizationHeader, postId));
        verify(voteRepository, never()).deleteById(any());
    }

    @Test
    void createVote_InvalidJwtToken_ThrowsException() throws InvalidCredentialsException {
        when(jwtService.getUserIdFromAuthorizationHeader(anyString()))
                .thenThrow(new InvalidCredentialsException("Invalid token"));

        assertThrows(InvalidCredentialsException.class, () -> voteService.createVote(authorizationHeader, postId, true));
        verify(voteRepository, never()).save(any(Vote.class));
    }

    @Test
    void cancelVote_InvalidJwtToken_ThrowsException() throws InvalidCredentialsException {
        when(jwtService.getUserIdFromAuthorizationHeader(anyString()))
                .thenThrow(new InvalidCredentialsException("Invalid token"));

        assertThrows(InvalidCredentialsException.class, () -> voteService.cancelVote(authorizationHeader, postId));
        verify(voteRepository, never()).deleteById(any());
    }

    @Test
    void createVote_NullAuthorizationHeader_ThrowsException() throws InvalidCredentialsException {
        when(jwtService.getUserIdFromAuthorizationHeader(null))
                .thenThrow(new InvalidCredentialsException("Authorization header is invalid"));

        assertThrows(InvalidCredentialsException.class, () -> voteService.createVote(null, postId, true));
        verify(voteRepository, never()).save(any(Vote.class));
    }

    @Test
    void cancelVote_NullAuthorizationHeader_ThrowsException() throws InvalidCredentialsException {
        when(jwtService.getUserIdFromAuthorizationHeader(null))
                .thenThrow(new InvalidCredentialsException("Authorization header is invalid"));

        assertThrows(InvalidCredentialsException.class, () -> voteService.cancelVote(null, postId));
        verify(voteRepository, never()).deleteById(any());
    }

    @Test
    void createVote_EmptyAuthorizationHeader_ThrowsException() throws InvalidCredentialsException {
        when(jwtService.getUserIdFromAuthorizationHeader(""))
                .thenThrow(new InvalidCredentialsException("Authorization header is invalid"));

        assertThrows(InvalidCredentialsException.class, () -> voteService.createVote("", postId, true));
        verify(voteRepository, never()).save(any(Vote.class));
    }

    @Test
    void cancelVote_EmptyAuthorizationHeader_ThrowsException() throws InvalidCredentialsException {
        when(jwtService.getUserIdFromAuthorizationHeader(""))
                .thenThrow(new InvalidCredentialsException("Authorization header is invalid"));

        assertThrows(InvalidCredentialsException.class, () -> voteService.cancelVote("", postId));
        verify(voteRepository, never()).deleteById(any());
    }
}
