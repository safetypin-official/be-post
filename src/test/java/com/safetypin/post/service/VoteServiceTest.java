package com.safetypin.post.service;

import com.safetypin.post.model.Post;
import com.safetypin.post.model.Vote;
import com.safetypin.post.repository.PostRepository;
import com.safetypin.post.repository.VoteRepository;
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

    @InjectMocks
    private VoteServiceImpl voteService;

    private UUID userId;
    private UUID postId;
    private Post post;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        postId = UUID.randomUUID();
        post = new Post();
    }

    @Test
    void createVote_NewVote_Success() {
        when(postRepository.findById(postId)).thenReturn(Optional.of(post));
        when(voteRepository.findById(any())).thenReturn(Optional.empty());

        String result = voteService.createVote(userId, postId, true);

        assertEquals("Vote recorded successfully", result);
        verify(voteRepository, times(1)).save(any(Vote.class));
    }

    @Test
    void createVote_AlreadyUpvoted_NoChange() {
        when(postRepository.findById(postId)).thenReturn(Optional.of(post));

        Vote existingVote = new Vote(new Vote.VoteId(userId, post), true);
        when(voteRepository.findById(any())).thenReturn(Optional.of(existingVote));

        String result = voteService.createVote(userId, postId, true);

        assertEquals("User already up voted that post. Vote remains unchanged", result);
        verify(voteRepository, never()).save(any(Vote.class));
    }

    @Test
    void createVote_AlreadyDownvoted_NoChange() {
        when(postRepository.findById(postId)).thenReturn(Optional.of(post));

        Vote existingVote = new Vote(new Vote.VoteId(userId, post), false);
        when(voteRepository.findById(any())).thenReturn(Optional.of(existingVote));

        String result = voteService.createVote(userId, postId, false);

        assertEquals("User already down voted that post. Vote remains unchanged", result);
        verify(voteRepository, never()).save(any(Vote.class));
    }

    @Test
    void createVote_ChangeFromUpvoteToDownvote_Success() {
        when(postRepository.findById(postId)).thenReturn(Optional.of(post));

        Vote existingVote = new Vote(new Vote.VoteId(userId, post), true);
        when(voteRepository.findById(any())).thenReturn(Optional.of(existingVote));

        String result = voteService.createVote(userId, postId, false);

        assertEquals("Vote recorded successfully", result);
        verify(voteRepository, times(1)).save(any(Vote.class));
    }

    @Test
    void createVote_ChangeFromDownvoteToUpvote_Success() {
        when(postRepository.findById(postId)).thenReturn(Optional.of(post));

        Vote existingVote = new Vote(new Vote.VoteId(userId, post), false);
        when(voteRepository.findById(any())).thenReturn(Optional.of(existingVote));

        String result = voteService.createVote(userId, postId, true);

        assertEquals("Vote recorded successfully", result);
        verify(voteRepository, times(1)).save(any(Vote.class));
    }

    @Test
    void cancelVote_Success() {
        when(postRepository.findById(postId)).thenReturn(Optional.of(post));

        Vote existingVote = new Vote(new Vote.VoteId(userId, post), true);
        when(voteRepository.findById(any())).thenReturn(Optional.of(existingVote));

        String result = voteService.cancelVote(userId, postId);

        assertEquals("Vote cancelled successfully", result);
        verify(voteRepository, times(1)).deleteById(any());
    }

    @Test
    void cancelVote_NoVoteExists_NoChange() {
        when(postRepository.findById(postId)).thenReturn(Optional.of(post));
        when(voteRepository.findById(any())).thenReturn(Optional.empty());

        String result = voteService.cancelVote(userId, postId);

        assertEquals("User hasn't voted that post. Vote remains unchanged", result);
        verify(voteRepository, never()).deleteById(any());
    }

    @Test
    void createVote_PostNotFound_ThrowsException() {
        when(postRepository.findById(postId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> voteService.createVote(userId, postId, true));
        verify(voteRepository, never()).save(any(Vote.class));
    }

    @Test
    void cancelVote_PostNotFound_ThrowsException() {
        when(postRepository.findById(postId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> voteService.cancelVote(userId, postId));
        verify(voteRepository, never()).deleteById(any());
    }
}
