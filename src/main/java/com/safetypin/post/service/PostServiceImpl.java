package com.safetypin.post.service;

import com.safetypin.post.exception.InvalidPostDataException;
import com.safetypin.post.exception.PostException;
import com.safetypin.post.model.Category;
import com.safetypin.post.model.Post;
import com.safetypin.post.repository.PostRepository;
import com.safetypin.post.service.filter.*;
import com.safetypin.post.utils.DistanceCalculator;
import lombok.extern.slf4j.Slf4j;
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
            Category category, LocalDateTime dateFrom, LocalDateTime dateTo,
            Pageable pageable) {

        // create point
        Point centerPoint = geometryFactory.createPoint(new Coordinate(centerLon, centerLat));

        // get all posts within radius with page
        Page<Post> posts = postRepository.findPostsWithFilter(
                centerPoint, radius, category, dateFrom, dateTo, pageable);

        // add distance to each posts
        return posts.map(post -> {
            Map<String, Object> result = new HashMap<>();
            result.put("post", post);
            Point postLocation = post.getLocation();
            if (postLocation != null) {
                double distance = DistanceCalculator.calculateDistance(
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
        log.info(center.toString());
        return postRepository.findPostsWithinPointAndRadius(center, radius, pageable);
    }

    // only for test
    @Override
    public List<Post> getPostsWithinRadius(double latitude, double longitude, double radius) {
        List<Post> allPosts = postRepository.findAll();
        return allPosts.stream()
                .filter(post -> {
                    if (post.getLocation() == null) return false;
                    double distance = DistanceCalculator.calculateDistance(latitude, longitude,
                            post.getLocation().getY(), post.getLocation().getX());
                    return distance <= radius;
                })
                .toList();
    }

    // deprecated, use findPostByLocation instead
    @Override
    public List<Post> getPostsByCategory(Category category) {
        return postRepository.findByCategory(category);
    }

    // deprecated, use findPostByLocation instead
    @Override
    public List<Post> getPostsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return postRepository.findByCreatedAtBetween(startDate, endDate);
    }

    // only for test
    @Override
    public List<Post> getPostsWithFilters(double latitude, double longitude, double radius,
                                          Category category, LocalDateTime startDate, LocalDateTime endDate) {
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

        // Add date range filtering strategy if both dates are provided
        if (startDate != null && endDate != null) {
            compositeStrategy.addStrategy(new DateRangeFilteringStrategy(startDate, endDate));
        }

        // Apply all strategies to all posts
        return filterPosts(compositeStrategy);
    }

    // only for test
    @Override
    public List<Post> getPostsByProximity(double latitude, double longitude) {
        List<Post> allPosts = postRepository.findAll();
        return allPosts.stream()
                .filter(post -> post.getLocation() != null)
                .sorted((post1, post2) -> {
                    double dist1 = DistanceCalculator.calculateDistance(latitude, longitude,
                            post1.getLocation().getY(), post1.getLocation().getX());
                    double dist2 = DistanceCalculator.calculateDistance(latitude, longitude,
                            post2.getLocation().getY(), post2.getLocation().getX());
                    return Double.compare(dist1, dist2);
                })
                .toList();
    }

    @Override
    public Post createPost(String title, String content, Double latitude, Double longitude, Category category) {
        if (title == null || title.trim().isEmpty()) {
            throw new InvalidPostDataException("Post title is required");
        }

        if (content == null || content.trim().isEmpty()) {
            throw new InvalidPostDataException("Post content is required");
        }

        if (latitude == null || longitude == null) {
            throw new InvalidPostDataException("Location coordinates are required");
        }

        try {
            Post post = new Post();
            post.setTitle(title);
            post.setCaption(content);
            post.setCategory(category);
            post.setCreatedAt(LocalDateTime.now());

            Point location = geometryFactory.createPoint(new Coordinate(longitude, latitude));
            post.setLocation(location);

            return postRepository.save(post);
        } catch (Exception e) {
            throw new PostException("Failed to create post: " + e.getMessage(), "POST_CREATION_ERROR", e);
        }
    }

    // Implement the new strategy-based filtering method
    @Override
    public List<Post> filterPosts(PostFilteringStrategy filterStrategy) {
        List<Post> allPosts = postRepository.findAll();
        return filterStrategy.filter(allPosts);
    }

}