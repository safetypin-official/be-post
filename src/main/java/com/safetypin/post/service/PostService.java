package com.safetypin.post.service;

import com.safetypin.post.dto.LocationFilter;
import com.safetypin.post.model.Post;
import org.apache.hc.client5.http.auth.InvalidCredentialsException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public interface PostService {

        Page<Map<String, Object>> findPostsByLocation(
                        Double centerLat, Double centerLon, Double radius,
                        LocationFilter filter, String authorizationHeader,
                        Pageable pageable) throws InvalidCredentialsException;

        Post createPost(String title, String content, Double latitude, Double longitude, String category,
                        UUID postedBy);

        List<Post> findAll(); // debugging purposes

        Page<Post> findAllPaginated(String authorizationHeader, Pageable pageable);

        Page<Map<String, Object>> findPostsByDistanceFeed(
                        Double userLat, Double userLon, String authorizationHeader, Pageable pageable)
                        throws InvalidCredentialsException;

        Post findById(UUID id);

        Page<Map<String, Object>> searchPosts(
                        Double centerLat, Double centerLon, Double radius,
                        String keyword, List<String> categories, String authorizationHeader, Pageable pageable)
                        throws InvalidCredentialsException;

        Page<Map<String, Object>> findPostsByTimestampFeed(String authorizationHeader, Pageable pageable)
                        throws InvalidCredentialsException;

        // New method to delete a post by ID, with user validation
        void deletePost(UUID postId, UUID userId);
}
