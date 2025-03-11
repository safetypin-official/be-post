package com.safetypin.post.service.filter;

import com.safetypin.post.model.Post;
import com.safetypin.post.utils.DistanceCalculator;
import java.util.List;
import java.util.stream.Collectors;

public class LocationRadiusFilteringStrategy implements PostFilteringStrategy {
    
    private final double latitude;
    private final double longitude;
    private final double radius;
    
    public LocationRadiusFilteringStrategy(double latitude, double longitude, double radius) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.radius = radius;
    }
    
    @Override
    public List<Post> filter(List<Post> posts) {
        return posts.stream()
                .filter(post -> {
                    if (post.getLocation() == null) return false;
                    double distance = DistanceCalculator.calculateDistance(
                            latitude, longitude,
                            post.getLocation().getY(), post.getLocation().getX());
                    return distance <= radius;
                })
                .collect(Collectors.toList());
    }
}
