package com.safetypin.post.service;

import com.safetypin.post.exception.InvalidPostDataException;
import com.safetypin.post.exception.PostException;
import com.safetypin.post.model.Category;
import com.safetypin.post.model.Post;
import com.safetypin.post.repository.CategoryRepository;
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
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PostServiceImpl implements PostService {

    private final PostRepository postRepository;
    private final CategoryRepository categoryRepository;
    private final GeometryFactory geometryFactory;

    @Autowired
    public PostServiceImpl(PostRepository postRepository, CategoryRepository categoryRepository, GeometryFactory geometryFactory) {
        this.postRepository = postRepository;
        this.categoryRepository = categoryRepository;
        this.geometryFactory = geometryFactory;
    }

    // find all (debugging purposes)
    @Override
    public List<Post> findAll() {
        return postRepository.findAll();
    }

    // find all with pagination
    @Override
    public Page<Post> findAllPaginated(Pageable pageable) {
        return postRepository.findAll(pageable);
    }

    // find posts given filter
    @Override
    public Page<Map<String, Object>> findPostsByLocation(
            Double centerLat, Double centerLon, Double radius,
            String category, LocalDateTime dateFrom, LocalDateTime dateTo,
            Pageable pageable) {
        Point center = geometryFactory.createPoint(new Coordinate(centerLon, centerLat));
        Page<Post> postsPage;

        // Use a larger radius for database query to account for calculation differences
        // The exact filtering will be done after calculating the actual distances
        Double radiusInMeters = radius * 1000;

        // Select appropriate query based on date parameters only, we'll filter by category later
        if (dateFrom != null && dateTo != null) {
            // Filter by date range only
            postsPage = postRepository.findPostsByDateRange(
                    center, radiusInMeters, dateFrom, dateTo, pageable);
        } else {
            // No date filters, just use radius
            postsPage = postRepository.findPostsWithinRadius(center, radiusInMeters, pageable);
        }

        // Check if postsPage is null, return empty page if null
        if (postsPage == null) {
            return new PageImpl<>(Collections.emptyList(), pageable, 0);
        }

        // Transform Post entities to DTOs with distance, filtering by actual calculated distance and category
        List<Map<String, Object>> filteredResults = postsPage.getContent().stream()
                .map(post -> {
                    Map<String, Object> result = new HashMap<>();

                    // Create post data with category name
                    Map<String, Object> postData = new HashMap<>();
                    postData.put("id", post.getId());
                    postData.put("title", post.getTitle());
                    postData.put("caption", post.getCaption());
                    postData.put("latitude", post.getLatitude());
                    postData.put("longitude", post.getLongitude());
                    postData.put("createdAt", post.getCreatedAt());
                    postData.put("category", post.getCategory()); // Now directly using the category name

                    result.put("post", postData);

                    double distance = 0.0;
                    if (post.getLocation() != null) {
                        distance = DistanceCalculator.calculateDistance(
                                centerLat, centerLon, post.getLatitude(), post.getLongitude()
                        );
                        result.put("distance", distance);
                    } else {
                        result.put("distance", distance);
                    }

                    // Return the result with the calculated distance and category name for filtering
                    return new AbstractMap.SimpleEntry<>(result, Map.entry(distance, post.getCategory()));
                })
                // Filter by the actual calculated distance (using the specified radius)
                .filter(entry -> entry.getValue().getKey() != null && entry.getValue().getKey() <= radiusInMeters / 1000)
                // Filter by category if provided
                .filter(entry -> category == null ||
                        (entry.getValue().getValue() != null &&
                                entry.getValue().getValue().equalsIgnoreCase(category)))
                // Extract just the result map
                .map(AbstractMap.SimpleEntry::getKey)
                .collect(Collectors.toList());

        // Create a new page with the filtered results
        return new PageImpl<>(
                filteredResults,
                pageable,
                filteredResults.size()
        );
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

        // Verify that the category exists
        Category categoryObj = categoryRepository.findByName(category);
        if (categoryObj == null) {
            throw new InvalidPostDataException("Category does not exist: " + category);
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