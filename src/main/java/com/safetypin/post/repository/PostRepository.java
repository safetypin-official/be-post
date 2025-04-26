package com.safetypin.post.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.safetypin.post.model.Post;

@Repository
public interface PostRepository extends JpaRepository<Post, UUID> {
    Page<Post> findByPostedByOrderByCreatedAtDesc(UUID postedBy, Pageable pageable);

    // Added method to find posts by a list of user IDs
    List<Post> findByPostedByIn(List<UUID> postedBy);
}
