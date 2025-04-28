package com.safetypin.post.repository;

import com.safetypin.post.model.CommentOnPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface CommentOnPostRepository extends JpaRepository<CommentOnPost, UUID> {
    List<CommentOnPost> findByParentId(UUID parentId);

    // Find comments on posts owned by a specific user within a time range
    @Query("SELECT c FROM CommentOnPost c WHERE c.parent.postedBy = :userId AND c.postedBy <> :userId AND c.createdAt >= :since")
    List<CommentOnPost> findCommentsOnUserPostsSince(@Param("userId") UUID userId, @Param("since") LocalDateTime since);

    // Find comments made by a specific user within a time range
    List<CommentOnPost> findByPostedByAndCreatedAtGreaterThanEqual(UUID postedBy, LocalDateTime since);

    // fetch comments in profile
    @Query("SELECT c FROM CommentOnPost c WHERE c.postedBy = :userId")
    List<CommentOnPost> findCommentsByPostedBy(@Param("userId") UUID userId);
}
