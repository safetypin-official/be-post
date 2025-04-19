package com.safetypin.post.repository;

import com.safetypin.post.model.CommentOnPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CommentOnPostRepository extends JpaRepository<CommentOnPost, UUID> {
    List<CommentOnPost> findByParentId(UUID parentId);
}
