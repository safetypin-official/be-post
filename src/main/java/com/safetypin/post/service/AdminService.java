package com.safetypin.post.service;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.safetypin.post.exception.UnauthorizedAccessException;
import com.safetypin.post.model.Role;
import com.safetypin.post.repository.CommentOnCommentRepository;
import com.safetypin.post.repository.CommentOnPostRepository;
import com.safetypin.post.repository.PostRepository;
import com.safetypin.post.repository.VoteRepository;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@AllArgsConstructor
public class AdminService {

    private final PostRepository postRepository;
    private final CommentOnPostRepository commentOnPostRepository;
    private final CommentOnCommentRepository commentOnCommentRepository;
    private final VoteRepository voteRepository;

    /**
     * Asynchronously deletes all content created by a specific user.
     * This includes posts, comments on posts, comments on comments, and votes.
     * 
     * @param userId        The ID of the user whose content should be deleted
     * @param moderatorId   The ID of the moderator who is performing the deletion
     * @param moderatorRole The role of the moderator performing the deletion
     * @return A CompletableFuture that will be completed when the deletion is done
     * @throws UnauthorizedAccessException if the user performing the deletion is
     *                                     not a MODERATOR
     */
    @Async
    @Transactional
    public CompletableFuture<Void> deleteAllUserContent(UUID userId, UUID moderatorId, Role moderatorRole) {
        // Check if the user is authorized to perform this operation
        if (moderatorRole != Role.MODERATOR) {
            log.error("Unauthorized deletion attempt by user {}: not a moderator", moderatorId);
            throw new UnauthorizedAccessException("Only moderators can delete user content");
        }

        log.info("Starting asynchronous deletion of all content for user {} by moderator {}", userId, moderatorId);

        try {
            // The deletion order is important to maintain referential integrity

            // 1. Delete votes by the user (no dependencies)
            voteRepository.deleteVotesByUserId(userId);

            // 2. Delete comments on comments (child comments)
            commentOnCommentRepository.deleteByPostedBy(userId);

            // 3. Delete comments on posts (parent comments)
            commentOnPostRepository.deleteByPostedBy(userId);

            // 4. Delete posts
            postRepository.deleteByPostedBy(userId);

            log.info("Successfully completed deletion of all content for user {} by moderator {}", userId, moderatorId);
            return CompletableFuture.completedFuture(null);

        } catch (Exception e) {
            log.error("Error while deleting content for user {}: {}", userId, e.getMessage(), e);
            throw e;
        }
    }
}
