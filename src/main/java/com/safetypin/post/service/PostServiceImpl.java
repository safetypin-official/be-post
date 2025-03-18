package com.safetypin.post.service;

import com.safetypin.post.exception.InvalidPostDataException;
import com.safetypin.post.exception.PostException;
import com.safetypin.post.model.Post;
import com.safetypin.post.repository.PostRepository;
import com.safetypin.post.utils.DistanceCalculator;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class PostServiceImpl implements PostService {

    private final PostRepository postRepository;
    private final GeometryFactory geometryFactory;

    @Autowired
    public PostServiceImpl(PostRepository postRepository, GeometryFactory geometryFactory) {
        this.postRepository = postRepository;
        this.geometryFactory = geometryFactory;
    }

    // find all (debugging purposes)
    @Override
    public List<Post> findAll() {
        return postRepository.findAll();
    }

    // find posts given filter
    @Override
    public Page<Map<String, Object>> findPostsByLocation(
            Double centerLat, Double centerLon, Double radius,
            String category, LocalDateTime dateFrom, LocalDateTime dateTo,
            Pageable pageable) {
        Point center = geometryFactory.createPoint(new Coordinate(centerLon, centerLat));
        Double radiusInMeters = radius * 1000;
        Page<Post> postsPage;

        if (category != null && dateFrom != null && dateTo != null) {
            postsPage = postRepository.findPostsWithFilter(center, radiusInMeters, category, dateFrom, dateTo, pageable);
        } else if (category != null) {
            postsPage = postRepository.findPostsWithinRadiusByCategory(center, radiusInMeters, category, pageable);
        } else if (dateFrom != null && dateTo != null) {
            postsPage = postRepository.findPostsWithinRadiusByDateRange(center, radiusInMeters, dateFrom, dateTo, pageable);
        } else {
            postsPage = postRepository.findPostsWithinPointAndRadius(center, radiusInMeters, pageable);
        }

        // Check if postsPage is null, return empty page if null
        if (postsPage == null) {
            return new PageImpl<>(Collections.emptyList(), pageable, 0);
        }

        // Transform Post entities to DTOs with distance
        return postsPage.map(post -> {
            Map<String, Object> result = new HashMap<>();
            result.put("post", post);

            if (post.getLocation() != null) {
                double distance = DistanceCalculator.calculateDistance(
                        centerLat, centerLon, post.getLatitude(), post.getLongitude()
                );
                result.put("distance", distance);
            } else {
                result.put("distance", 0.0);
            }
            return result;
        });
    }

    @Override
    public Post createPost(String title, String content, Double latitude, Double longitude, String category) {
        if (title == null || title.trim().isEmpty()) {
            throw new InvalidPostDataException("Title is required");
        }
        if (content == null || content.trim().isEmpty()) {
            throw new InvalidPostDataException("Content is required");
        }
        if (latitude == null || longitude == null) {
            throw new InvalidPostDataException("Location coordinates are required");
        }
        if (category == null || category.trim().isEmpty()) {
            throw new InvalidPostDataException("Category is required");
        }

        // Create the post
        Post post = new Post.Builder()
                .title(title)
                .caption(content)
                .location(latitude, longitude)
                .category(category)
                .build();

        try {
            return postRepository.save(post);
        } catch (Exception e) {
            log.error("Error saving post: {}", e.getMessage());
            throw new PostException("Failed to save the post: " + e.getMessage());
        }
    }
}