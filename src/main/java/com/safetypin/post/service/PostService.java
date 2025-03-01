package com.safetypin.post.service;

import com.safetypin.post.model.Post;
import org.locationtech.jts.geom.Point;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public interface PostService {
    
    /**
     * Find posts within a radius of a specified location
     * 
     * @param centerLat Latitude of the center point
     * @param centerLon Longitude of the center point
     * @param radius Radius in kilometers to search within
     * @param category Optional category filter
     * @param dateFrom Optional start date filter
     * @param dateTo Optional end date filter
     * @param pageable Pagination information
     * @return Page of posts with distance information
     */
    Page<Map<String, Object>> findPostsByLocation(
            Double centerLat, Double centerLon, Double radius, 
            String category, LocalDateTime dateFrom, LocalDateTime dateTo, 
            Pageable pageable);
    
    /**
     * Search for posts within a radius of a specified location
     * 
     * @param center Center point for the search
     * @param radius Radius in kilometers
     * @param pageable Pagination information
     * @return Page of posts
     */
    Page<Post> searchPostsWithinRadius(Point center, Double radius, Pageable pageable);
    
    List<Post> getPostsWithinRadius(double latitude, double longitude, double radius);
    
    List<Post> getPostsByCategory(String category);
    
    List<Post> getPostsByDateRange(LocalDateTime startDate, LocalDateTime endDate);
    
    List<Post> getPostsWithFilters(double latitude, double longitude, double radius, 
                               String category, LocalDateTime startDate, LocalDateTime endDate);
    
    List<Post> getPostsByProximity(double latitude, double longitude);
    
    List<Post> getPostsSortedByProximity();
}
