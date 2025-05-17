package com.safetypin.post.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.safetypin.post.exception.UnauthorizedAccessException;
import com.safetypin.post.model.Role;
import com.safetypin.post.repository.CommentOnCommentRepository;
import com.safetypin.post.repository.CommentOnPostRepository;
import com.safetypin.post.repository.PostRepository;
import com.safetypin.post.repository.VoteRepository;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AdminServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private CommentOnPostRepository commentOnPostRepository;

    @Mock
    private CommentOnCommentRepository commentOnCommentRepository;

    @Mock
    private VoteRepository voteRepository;

    private AdminService adminService;
    private UUID targetUserId;
    private UUID moderatorId;

    @BeforeEach
    void setup() {
        adminService = new AdminService(
                postRepository,
                commentOnPostRepository,
                commentOnCommentRepository,
                voteRepository);

        targetUserId = UUID.randomUUID();
        moderatorId = UUID.randomUUID();

        // Mock repository delete methods to return counts
        when(voteRepository.deleteVotesByUserId(any())).thenReturn(5);
        when(commentOnCommentRepository.deleteByPostedBy(any())).thenReturn(3);
        when(commentOnPostRepository.deleteByPostedBy(any())).thenReturn(2);
        when(postRepository.deleteByPostedBy(any())).thenReturn(1);
    }

    @Test
    void deleteAllUserContent_WithModeratorRole_DeletesAllContent()
            throws ExecutionException, InterruptedException {
        // Act
        CompletableFuture<Void> result = adminService.deleteAllUserContent(
                targetUserId,
                moderatorId,
                Role.MODERATOR);

        // Wait for the async operation to complete
        result.get();

        // Assert
        verify(voteRepository, times(1)).deleteVotesByUserId(targetUserId);
        verify(commentOnCommentRepository, times(1)).deleteByPostedBy(targetUserId);
        verify(commentOnPostRepository, times(1)).deleteByPostedBy(targetUserId);
        verify(postRepository, times(1)).deleteByPostedBy(targetUserId);
    }

    @Test
    void deleteAllUserContent_WithRegularUserRole_ThrowsException() {
        // Act & Assert
        assertThrows(UnauthorizedAccessException.class, () -> {
            adminService.deleteAllUserContent(
                    targetUserId,
                    moderatorId,
                    Role.REGISTERED_USER);
        });

        // Verify no deletion was performed
        verify(voteRepository, never()).deleteVotesByUserId(any());
        verify(commentOnCommentRepository, never()).deleteByPostedBy(any());
        verify(commentOnPostRepository, never()).deleteByPostedBy(any());
        verify(postRepository, never()).deleteByPostedBy(any());
    }

    @Test
    void deleteAllUserContent_WithPremiumUserRole_ThrowsException() {
        // Act & Assert
        assertThrows(UnauthorizedAccessException.class, () -> {
            adminService.deleteAllUserContent(
                    targetUserId,
                    moderatorId,
                    Role.PREMIUM_USER);
        });

        // Verify no deletion was performed
        verify(voteRepository, never()).deleteVotesByUserId(any());
        verify(commentOnCommentRepository, never()).deleteByPostedBy(any());
        verify(commentOnPostRepository, never()).deleteByPostedBy(any());
        verify(postRepository, never()).deleteByPostedBy(any());
    }

    @Test
    void deleteAllUserContent_WithRepositoryException_PropagatesException() {
        // Arrange
        when(voteRepository.deleteVotesByUserId(any())).thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            CompletableFuture<Void> result = adminService.deleteAllUserContent(
                    targetUserId,
                    moderatorId,
                    Role.MODERATOR);

            // Wait for the async operation to complete
            result.get();
        });

        // Verify that only the first repository method was called
        verify(voteRepository, times(1)).deleteVotesByUserId(targetUserId);
        verify(commentOnCommentRepository, never()).deleteByPostedBy(any());
        verify(commentOnPostRepository, never()).deleteByPostedBy(any());
        verify(postRepository, never()).deleteByPostedBy(any());
    }
}
