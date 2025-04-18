package com.safetypin.post.repository;

import com.safetypin.post.model.CommentOnComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CommentOnCommentRepository extends JpaRepository<CommentOnComment, UUID> {
    List<CommentOnComment> findByParentId(UUID parentId);
}
