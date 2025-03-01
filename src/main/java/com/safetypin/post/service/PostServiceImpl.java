package com.safetypin.post.service;

import com.safetypin.post.model.Post;
import com.safetypin.post.repository.PostRepository;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PostServiceImpl implements PostService {

    private final PostRepository postRepository;
    private final GeometryFactory geometryFactory;

    @Autowired
    public PostServiceImpl(PostRepository postRepository, GeometryFactory geometryFactory) {
        this.postRepository = postRepository;
        this.geometryFactory = geometryFactory;
    }

    @Override
    public Page<Map<String, Object>> findPostsByLocation(
            Double centerLat, Double centerLon, Double radius, 
            String category, LocalDateTime dateFrom, LocalDateTime dateTo, 
            Pageable pageable) {
        
        // Create a point from the given lat/lon
        Point centerPoint = geometryFactory.createPoint(new Coordinate(centerLon, centerLat));
        
        // Get posts within radius
        Page<Post> posts = searchPostsWithinRadius(centerPoint, radius, pageable);
        
        // Transform results to include distance info
        return posts.map(post -> {
            Map<String, Object> result = new HashMap<>();
            result.put("post", post);
            
            // Calculate distance
            Point postLocation = post.getLocation();
            if (postLocation != null) {
                double distance = calculateDistance(
                    centerLat, centerLon, 
                    postLocation.getY(), postLocation.getX()
                );
                result.put("distance", distance);
            } else {
                result.put("distance", null);
            }
            
            return result;
        });
    }

    @Override
    public Page<Post> searchPostsWithinRadius(Point center, Double radius, Pageable pageable) {
        // Implementation depends on your repository capabilities
        // This is a placeholder
        return postRepository.findWithinRadius(center, radius, pageable);
    }

    @Override
    public List<Post> getPostsWithinRadius(double latitude, double longitude, double radius) {
        List<Post> allPosts = postRepository.findAll();
        return allPosts.stream()
                .filter(post -> {
                    if (post.getLocation() == null) return false;
                    double distance = calculateDistance(latitude, longitude, 
                            post.getLocation().getY(), post.getLocation().getX());
                    return distance <= radius;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<Post> getPostsByCategory(String category) {
        return postRepository.findByCategory(category);
    }

    @Override
    public List<Post> getPostsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return postRepository.findByCreatedAtBetween(startDate, endDate);
    }

    @Override
    public List<Post> getPostsWithFilters(double latitude, double longitude, double radius,
                                    String category, LocalDateTime startDate, LocalDateTime endDate) {
        List<Post> allPosts = postRepository.findAll();
        return allPosts.stream()
                .filter(post -> {
                    if (post.getLocation() == null) return false;
                    double distance = calculateDistance(latitude, longitude, 
                            post.getLocation().getY(), post.getLocation().getX());
                    return distance <= radius;
                })
                .filter(post -> category == null || category.equals(post.getCategory()))
                .filter(post -> startDate == null || !post.getCreatedAt().isBefore(startDate))
                .filter(post -> endDate == null || !post.getCreatedAt().isAfter(endDate))
                .collect(Collectors.toList());
    }

    @Override
    public List<Post> getPostsByProximity(double latitude, double longitude) {
        List<Post> allPosts = postRepository.findAll();
        return allPosts.stream()
                .filter(post -> post.getLocation() != null)
                .sorted((post1, post2) -> {
                    double dist1 = calculateDistance(latitude, longitude, 
                            post1.getLocation().getY(), post1.getLocation().getX());
                    double dist2 = calculateDistance(latitude, longitude, 
                            post2.getLocation().getY(), post2.getLocation().getX());
                    return Double.compare(dist1, dist2);
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<Post> getPostsSortedByProximity() {
        // Retrieve all posts that have a non-null location
        List<Post> postsWithLocation = postRepository.findAll().stream()
                .filter(post -> post.getLocation() != null)
                .collect(Collectors.toList());
                
        if (postsWithLocation.isEmpty()) {
            return postsWithLocation;
        }
        
        // Calculate the centroid of all posts as the average of latitudes and longitudes
        double averageLat = postsWithLocation.stream()
                .mapToDouble(post -> post.getLocation().getY())
                .average()
                .orElse(0);
        double averageLon = postsWithLocation.stream()
                .mapToDouble(post -> post.getLocation().getX())
                .average()
                .orElse(0);
                
        // Sort posts by distance from the calculated centroid
        postsWithLocation.sort((post1, post2) -> {
            double distance1 = calculateDistance(
                    averageLat, averageLon,
                    post1.getLocation().getY(), post1.getLocation().getX()
            );
            double distance2 = calculateDistance(
                    averageLat, averageLon,
                    post2.getLocation().getY(), post2.getLocation().getX()
            );
            return Double.compare(distance1, distance2);
        });
        
        return postsWithLocation;
    }

    // Haversine formula to calculate distance between two points on Earth
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Radius of the earth in km
        
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return R * c;
    }
}
