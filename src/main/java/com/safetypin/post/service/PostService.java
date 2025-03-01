package com.safetypin.post.service;

import com.safetypin.post.model.Post;
import org.locationtech.jts.geom.Point;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface PostService {
    
    Page<Map<String, Object>> findPostsByLocation(
        Double latitude, 
        Double longitude, 
        Double radius, 
        String category,
        LocalDateTime dateFrom, 
        LocalDateTime dateTo, 
        PageRequest pageRequest
    );
    
    Page<Post> searchPostsWithinRadius(
        Point center, 
        Double radiusInMeters, 
        Pageable pageable
    );
    
    List<Post> getPostsWithinRadius(double latitude, double longitude, double radius);
    
    List<Post> getPostsByCategory(String category);
    
    List<Post> getPostsByDateRange(LocalDateTime startDate, LocalDateTime endDate);
    
    List<Post> getPostsWithFilters(double latitude, double longitude, double radius, 
                               String category, LocalDateTime startDate, LocalDateTime endDate);
    
    List<Post> getPostsByProximity(double latitude, double longitude);
    
    List<Post> getPostsSortedByProximity();
}
