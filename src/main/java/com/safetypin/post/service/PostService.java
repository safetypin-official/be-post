package com.safetypin.post.service;

import com.safetypin.post.model.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public interface PostService {

        Post createPost(String title, String content, Double latitude, Double longitude, String category,
                        UUID postedBy);

        List<Post> findAll(); // debugging purposes

        Page<Post> findAllPaginated(UUID userId, Pageable pageable);

        Page<Map<String, Object>> findPostsByDistanceFeed(
                        Double userLat, Double userLon,
                        List<String> categories, String keyword,
                        LocalDateTime dateFrom, LocalDateTime dateTo,
                        UUID userId, Pageable pageable);

        Post findById(UUID id);

        Page<Map<String, Object>> findPostsByTimestampFeed(
                        List<String> categories, String keyword,
                        LocalDateTime dateFrom, LocalDateTime dateTo,
                        UUID userId, Pageable pageable);

        Page<Map<String, Object>> findPostsByUser(UUID userId, Pageable pageable);

        // New method to delete a post by ID, with user validation
        void deletePost(UUID postId, UUID userId);
}
