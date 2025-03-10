package com.safetypin.post.service;

import com.safetypin.post.model.Category;
import com.safetypin.post.model.Post;
import org.locationtech.jts.geom.Point;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public interface PostService {

    Page<Map<String, Object>> findPostsByLocation(
            Double centerLat, Double centerLon, Double radius,
            Category category, LocalDateTime dateFrom, LocalDateTime dateTo,
            Pageable pageable);

    Page<Post> searchPostsWithinRadius(Point center, Double radius, Pageable pageable);

    List<Post> getPostsWithinRadius(double latitude, double longitude, double radius);

    List<Post> getPostsByCategory(Category category);

    List<Post> getPostsByDateRange(LocalDateTime startDate, LocalDateTime endDate);

    List<Post> getPostsWithFilters(double latitude, double longitude, double radius,
                                   Category category, LocalDateTime startDate, LocalDateTime endDate);

    List<Post> getPostsByProximity(double latitude, double longitude);

    Post createPost(String title, String content, Double latitude, Double longitude, Category category);

    List<Post> findAll();
}