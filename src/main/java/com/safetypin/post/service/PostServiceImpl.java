package com.safetypin.post.service;

import com.safetypin.post.exception.InvalidPostDataException;
import com.safetypin.post.exception.PostException;
import com.safetypin.post.exception.PostNotFoundException;
import com.safetypin.post.model.Category;
import com.safetypin.post.model.Post;
import com.safetypin.post.repository.CategoryRepository;
import com.safetypin.post.repository.PostRepository;
import com.safetypin.post.utils.DistanceCalculator;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.auth.InvalidCredentialsException;
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

@Slf4j
@Service
public class PostServiceImpl implements PostService {

    private static final String DISTANCE_KEY = "distance";
    private final PostRepository postRepository;
    private final CategoryRepository categoryRepository;
    private final GeometryFactory geometryFactory;
    private final JwtService jwtService;

    @Autowired
    public PostServiceImpl(PostRepository postRepository, CategoryRepository categoryRepository,
                           GeometryFactory geometryFactory, JwtService jwtService) {
        this.postRepository = postRepository;
        this.categoryRepository = categoryRepository;
        this.geometryFactory = geometryFactory;
        this.jwtService = jwtService;
    }

    // Change from private to protected for testing
    protected Map<String, Object> mapPostToData(Post post, UUID userId) {
        Map<String, Object> postData = new HashMap<>();
        postData.put("id", post.getId());
        postData.put("title", post.getTitle());
        postData.put("caption", post.getCaption());
        postData.put("latitude", post.getLatitude());
        postData.put("longitude", post.getLongitude());
        postData.put("createdAt", post.getCreatedAt());
        postData.put("category", post.getCategory());
        postData.put("upvoteCount", post.getUpvoteCount());
        postData.put("downvoteCount", post.getDownvoteCount());
        postData.put("currentVote", post.currentVote(userId));
        postData.put("postedBy", post.getPostedBy()); // Add postedBy to response
        return postData;
    }

    // find all (debugging purposes)
    @Override
    public List<Post> findAll() {
        return postRepository.findAll();
    }

    // find all with pagination
    @Override
    public Page<Post> findAllPaginated(String authorizationHeader, Pageable pageable) {
        return postRepository.findAll(pageable);
    }

    // find posts given filter
    @Override
    public Page<Map<String, Object>> findPostsByLocation(
            Double centerLat, Double centerLon, Double radius,
            String category, LocalDateTime dateFrom, LocalDateTime dateTo,
            String authorizationHeader, Pageable pageable) throws InvalidCredentialsException {
        UUID userId = jwtService.getUserIdFromAuthorizationHeader(authorizationHeader);
        Point center = geometryFactory.createPoint(new Coordinate(centerLon, centerLat));
        Page<Post> postsPage;

        // Use a larger radius for database query to account for calculation differences
        // The exact filtering will be done after calculating the actual distances
        Double radiusInMeters = radius * 1000;

        // Select appropriate query based on date parameters only, we'll filter by
        // category later
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

        // Transform Post entities to DTOs with distance, filtering by actual calculated
        // distance and category
        List<Map<String, Object>> filteredResults = postsPage.getContent().stream()
                .filter(Objects::nonNull) // Filter out null posts
                .map(post -> {
                    Map<String, Object> result = new HashMap<>();

                    // Use helper method instead of duplicated code
                    Map<String, Object> postData = mapPostToData(post, userId);
                    result.put("post", postData);

                    double distance = 0.0;
                    if (post.getLocation() != null) {
                        distance = DistanceCalculator.calculateDistance(
                                centerLat, centerLon, post.getLatitude(), post.getLongitude());
                    }
                    result.put(DISTANCE_KEY, distance);

                    // Return the result with the calculated distance and category name for
                    // filtering
                    return new AbstractMap.SimpleEntry<>(result, Map.entry(distance, post.getCategory()));
                })
                // Filter by the actual calculated distance (using the specified radius)
                // Add a check to exclude posts with null locations
                .filter(entry -> {
                    if (entry.getValue().getKey() == null || entry.getValue().getKey() > radiusInMeters / 1000)
                        return false;
                    Object postObj = entry.getKey().get("post");
                    Map<?, ?> postMap = (Map<?, ?>) postObj;
                    return postMap.get("latitude") != null && postMap.get("longitude") != null;
                })
                // Filter by category if provided
                .filter(entry -> category == null ||
                        (entry.getValue().getValue() != null &&
                                entry.getValue().getValue().equalsIgnoreCase(category)))
                // Extract just the result map
                .map(AbstractMap.SimpleEntry::getKey)
                .toList();

        // Create a new page with the filtered results
        return new PageImpl<>(
                filteredResults,
                pageable,
                filteredResults.size());
    }

    @Override
    public Page<Map<String, Object>> findPostsByDistanceFeed(Double userLat, Double userLon, String authorizationHeader,
                                                             Pageable pageable) throws InvalidCredentialsException {

        UUID userId = jwtService.getUserIdFromAuthorizationHeader(authorizationHeader);

        // Get all posts
        List<Post> allPosts = postRepository.findAll();

        // Calculate distance and create result objects
        List<Map<String, Object>> postsWithDistance = allPosts.stream()
                .map(post -> {
                    Map<String, Object> result = new HashMap<>();

                    // Use helper method instead of duplicated code
                    Map<String, Object> postData = mapPostToData(post, userId);
                    result.put("post", postData);

                    // Calculate distance from user
                    double distance = DistanceCalculator.calculateDistance(
                            userLat, userLon, post.getLatitude(), post.getLongitude());
                    result.put(DISTANCE_KEY, distance);

                    return result;
                })
                // Sort by distance (nearest first)
                .sorted(Comparator.comparingDouble(post -> (Double) post.get(DISTANCE_KEY)))
                .toList();

        // Manual pagination
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), postsWithDistance.size());

        // Create sub-list for current page - handle case when start might be out of
        // bounds
        List<Map<String, Object>> pageContent = start >= postsWithDistance.size() ? Collections.emptyList()
                : postsWithDistance.subList(start, end);

        // Return paginated result
        return new PageImpl<>(pageContent, pageable, postsWithDistance.size());
    }

    @Override
    public Page<Map<String, Object>> findPostsByTimestampFeed(String authorizationHeader, Pageable pageable)
            throws InvalidCredentialsException {
        UUID userId = jwtService.getUserIdFromAuthorizationHeader(authorizationHeader);
        // Get posts sorted by timestamp (newest first)
        Page<Post> postsPage = postRepository.findAll(pageable);

        // Transform Post entities to response format
        List<Map<String, Object>> formattedPosts = postsPage.getContent().stream()
                .map(post -> {
                    Map<String, Object> result = new HashMap<>();

                    // Use helper method instead of duplicated code
                    Map<String, Object> postData = mapPostToData(post, userId);
                    result.put("post", postData);

                    return result;
                })
                .sorted(Comparator.comparing(
                        post -> ((LocalDateTime) ((Map<String, Object>) post.get("post")).get("createdAt")),
                        Comparator.reverseOrder()))
                .toList();

        // Return paginated result
        return new PageImpl<>(formattedPosts, pageable, postsPage.getTotalElements());
    }

    @Override
    public Post createPost(String title, String content, Double latitude, Double longitude, String category,
                           UUID postedBy) {
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
        if (postedBy == null) {
            throw new InvalidPostDataException("User ID (postedBy) is required");
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
                .postedBy(postedBy) // Set the postedBy value
                .build();

        try {
            return postRepository.save(post);
        } catch (Exception e) {
            log.error("Error saving post: {}", e.getMessage());
            throw new PostException("Failed to save the post: " + e.getMessage());
        }
    }

    @Override
    public Post findById(UUID id) {
        return postRepository.findById(id)
                .orElseThrow(() -> new PostNotFoundException("Post not found with id: " + id));
    }

    @Override
    public Page<Map<String, Object>> searchPosts(
            Double centerLat, Double centerLon, Double radius,
            String keyword, List<String> categories, String authorizationHeader,
            Pageable pageable) {

        // Check if we have any search criteria
        boolean hasKeyword = keyword != null && !keyword.trim().isEmpty();
        boolean hasCategories = categories != null && !categories.isEmpty();

        // No search criteria provided, return empty result
        if (!hasKeyword && !hasCategories) {
            return new PageImpl<>(Collections.emptyList(), pageable, 0);
        }

        // Validate categories first if provided
        if (hasCategories) {
            validateCategories(categories);
        }

        // If we got here, categories are valid or none were provided
        // Now get userId since we have valid search criteria
        UUID userId;
        try {
            userId = jwtService.getUserIdFromAuthorizationHeader(authorizationHeader);
        } catch (InvalidCredentialsException | RuntimeException e) {
            throw new PostException("Authentication error while searching posts: " + e.getMessage(), e);
        }

        Point center = geometryFactory.createPoint(new Coordinate(centerLon, centerLat));
        Double radiusInMeters = radius * 1000;
        Page<Post> postsPage;

        if (hasCategories) {
            if (hasKeyword) {
                // Case 3: Both keyword and categories provided
                postsPage = postRepository.searchPostsByKeywordAndCategories(
                        center, radiusInMeters, keyword, categories, pageable);
            } else {
                // Case 2: Only categories provided
                postsPage = postRepository.searchPostsByCategories(
                        center, radiusInMeters, categories, pageable);
            }
        } else {
            // Case 1: Only keyword provided
            postsPage = postRepository.searchPostsByKeyword(
                    center, radiusInMeters, keyword, pageable);
        }

        if (postsPage == null) {
            return new PageImpl<>(Collections.emptyList(), pageable, 0);
        }

        // Process results similar to findPostsByLocation
        List<Map<String, Object>> results = postsPage.getContent().stream()
                .map(post -> {
                    Map<String, Object> result = new HashMap<>();
                    Map<String, Object> postData = mapPostToData(post, userId);
                    result.put("post", postData);

                    double distance = 0.0;
                    // Check if post has a valid location with lat/long

                    distance = DistanceCalculator.calculateDistance(
                            centerLat, centerLon, post.getLatitude(), post.getLongitude());

                    result.put(DISTANCE_KEY, distance);

                    return result;
                })
                .filter(result -> (Double) result.get(DISTANCE_KEY) <= radius) // Filter by actual radius in km
                .toList();

        return new PageImpl<>(results, pageable, results.size());
    }

    // Helper method to validate that all categories exist
    private void validateCategories(List<String> categories) {
        for (String category : categories) {
            Category categoryObj = categoryRepository.findByName(category);
            if (categoryObj == null) {
                throw new InvalidPostDataException("Category does not exist: " + category);
            }
        }
    }
}