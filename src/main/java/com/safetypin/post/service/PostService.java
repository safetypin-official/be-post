package com.safetypin.post.service;

import com.safetypin.post.dto.FeedQueryDTO;
import com.safetypin.post.dto.PostCreateRequest;
import com.safetypin.post.model.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public interface PostService {

    Post createPost(PostCreateRequest postCreateRequest);

    List<Post> findAll(); // debugging purposes

    Page<Post> findAllPaginated(Pageable pageable);

    Post findById(UUID id);

    Page<Map<String, Object>> findPostsByUser(UUID userId, Pageable pageable);

    // New method to delete a post by ID, with user validation
    void deletePost(UUID postId, UUID userId);

    // Method using strategy pattern for feed algorithms
    Page<Map<String, Object>> getFeed(FeedQueryDTO queryDTO, String feedType);
}
