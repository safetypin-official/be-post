package com.safetypin.post.repository;

import com.safetypin.post.model.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface PostRepository extends JpaRepository<Post, UUID> {
    List<Post> findByCategory(String category);

    List<Post> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    List<Post> findByPostedByOrderByCreatedAtDesc(UUID postedBy);
}
