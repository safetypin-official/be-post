package com.safetypin.post.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.safetypin.post.model.CommentOnComment;

@Repository
public interface CommentOnCommentRepository extends JpaRepository<CommentOnComment, UUID> {
        List<CommentOnComment> findByParentId(UUID parentId);

        // Find replies to comments owned by a specific user within a time range
        @Query("SELECT r FROM CommentOnComment r WHERE r.parent.postedBy = :userId AND r.postedBy <> :userId AND r.createdAt >= :since")
        List<CommentOnComment> findRepliesToUserCommentsSince(@Param("userId") UUID userId,
                        @Param("since") LocalDateTime since);

        // Find replies made by a specific user within a time range
        List<CommentOnComment> findByPostedByAndCreatedAtGreaterThanEqual(UUID postedBy, LocalDateTime since);

        // Find replies by others on the same parent comment where the user also
        // replied, within a time range
        @Query("SELECT r FROM CommentOnComment r WHERE r.parent.id IN :parentCommentIds AND r.postedBy <> :userId AND r.createdAt >= :since")
        List<CommentOnComment> findSiblingRepliesSince(@Param("userId") UUID userId,
                        @Param("parentCommentIds") List<UUID> parentCommentIds, @Param("since") LocalDateTime since);

        @Query("SELECT c FROM CommentOnComment c WHERE c.postedBy = :userId")
        List<CommentOnComment> findCommentsByPostedBy(@Param("userId") UUID userId);

        // Delete all replies made by a specific user
        @Modifying
        @Transactional
        @Query("DELETE FROM CommentOnComment c WHERE c.postedBy = :userId")
        int deleteByPostedBy(@Param("userId") UUID userId);
}
