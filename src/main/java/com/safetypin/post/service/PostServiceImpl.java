package com.safetypin.post.service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.locationtech.jts.geom.Point;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.safetypin.post.model.Post;
import com.safetypin.post.model.UserLocation;
import com.safetypin.post.repository.PostRepository;

@Service
public class PostServiceImpl implements PostService {

    private final PostRepository postRepository;
    private final UserLocationService userLocationService;
    
    public PostServiceImpl(PostRepository postRepository, UserLocationService userLocationService) {
        this.postRepository = postRepository;
        this.userLocationService = userLocationService;
    }

    @Override
    public Page<Map<String, Object>> findPostsByLocation(Double latitude, Double longitude, Double radius, 
            String category, LocalDateTime dateFrom, LocalDateTime dateTo, PageRequest pageRequest) {
        // Implementation needed
        return null;
    }
    
    @Override
    public Page<Post> searchPostsWithinRadius(Point center, Double radiusInMeters, Pageable pageable) {
        // Implementation needed
        return null;
    }

    @Override
    public List<Post> getPostsWithinRadius(double latitude, double longitude, double radius) {
        List<Post> allPosts = postRepository.findAll();
        return allPosts.stream()
            .filter(post -> calculateDistance(latitude, longitude, post.getLatitude(), post.getLongitude()) <= radius)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<Post> getPostsByCategory(String category) {
        return postRepository.findByCategory(category);
    }
    
    @Override
    public List<Post> getPostsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return postRepository.findByTimestampBetween(startDate, endDate);
    }
    
    @Override
    public List<Post> getPostsWithFilters(double latitude, double longitude, double radius, 
                                         String category, LocalDateTime startDate, LocalDateTime endDate) {
        List<Post> filteredByDateAndCategory = postRepository.findByTimestampBetweenAndCategory(startDate, endDate, category);
        
        return filteredByDateAndCategory.stream()
            .filter(post -> calculateDistance(latitude, longitude, post.getLatitude(), post.getLongitude()) <= radius)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<Post> getPostsByProximity(double latitude, double longitude) {
        List<Post> allPosts = postRepository.findAll();
        
        return allPosts.stream()
            .sorted(Comparator.comparingDouble(post -> 
                calculateDistance(latitude, longitude, post.getLatitude(), post.getLongitude())))
            .collect(Collectors.toList());
    }
    
    @Override
    public List<Post> getPostsSortedByProximity() {
        UserLocation userLocation = userLocationService.getCurrentUserLocation();
        List<Post> allPosts = postRepository.findAll();
        
        return allPosts.stream()
            .sorted(Comparator
                .<Post, Double>comparing(post -> calculateDistance(
                    userLocation.getLatitude(), userLocation.getLongitude(),
                    post.getLatitude(), post.getLongitude()))
                .thenComparing(Post::getCreatedAt, Comparator.reverseOrder()))
            .collect(Collectors.toList());
    }
    
    // Calculate distance using the Haversine formula
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Earth radius in km
        
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                 + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                 * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return R * c; // Distance in km
    }
}
