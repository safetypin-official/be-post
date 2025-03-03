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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
        Point centerPoint = geometryFactory.createPoint(new Coordinate(centerLon, centerLat));
        Page<Post> posts = searchPostsWithinRadius(centerPoint, radius, pageable);
        return posts.map(post -> {
            Map<String, Object> result = new HashMap<>();
            result.put("post", post);
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
        return postRepository.findPostsWithinPointAndRadius(center, radius, pageable);
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
        List<Post> candidatePosts;
        if (category != null && startDate != null && endDate != null) {
            candidatePosts = postRepository.findByTimestampBetweenAndCategory(startDate, endDate, category);
        } else if (category != null) {
            candidatePosts = postRepository.findByCategory(category);
        } else if (startDate != null && endDate != null) {
            candidatePosts = postRepository.findByCreatedAtBetween(startDate, endDate);
        } else {
            candidatePosts = postRepository.findAll();
        }
        return candidatePosts.stream()
                .filter(post -> {
                    if (post.getLocation() == null) return false;
                    double distance = calculateDistance(latitude, longitude,
                            post.getLocation().getY(), post.getLocation().getX());
                    return distance <= radius;
                })
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

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371;
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}