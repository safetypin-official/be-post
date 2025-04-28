package com.safetypin.post.repository;

import com.safetypin.post.model.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PostRepository extends JpaRepository<Post, UUID> {
    Page<Post> findByPostedByOrderByCreatedAtDesc(UUID postedBy, Pageable pageable);

    // Added method to find posts by a list of user IDs
    List<Post> findByPostedByIn(List<UUID> postedBy);

    @Query("SELECT COUNT(p) FROM Post p WHERE p.postedBy = :userId AND CAST(p.createdAt AS date) = CURRENT_DATE")
    int countPostsByUserToday(@Param("userId") UUID userId);

}
