package com.safetypin.post.service;

import com.safetypin.post.exception.InvalidPostDataException;
import com.safetypin.post.exception.PostException;
import com.safetypin.post.exception.PostNotFoundException;
import com.safetypin.post.exception.UnauthorizedAccessException;
import com.safetypin.post.model.Category;
import com.safetypin.post.model.Post;
import com.safetypin.post.repository.CategoryRepository;
import com.safetypin.post.repository.PostRepository;
import com.safetypin.post.utils.DistanceCalculator;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.auth.InvalidCredentialsException;
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
    private final JwtService jwtService;

    @Autowired
    public PostServiceImpl(PostRepository postRepository, CategoryRepository categoryRepository,
            JwtService jwtService) {
        this.postRepository = postRepository;
        this.categoryRepository = categoryRepository;
        this.jwtService = jwtService;
    }

    // Change from private to protected for testing
    protected Map<String, Object> mapPostToData(Post post, UUID userId) {
        Map<String, Object> postData = new HashMap<>();
        postData.put("title", post.getTitle());
        postData.put("caption", post.getCaption());
        postData.put("latitude", post.getLatitude());
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

    @Override
    public Page<Map<String, Object>> findPostsByTimestampFeed(String authorizationHeader, Pageable pageable)
            throws InvalidCredentialsException {

        if (authorizationHeader == null || authorizationHeader.isEmpty()) {
            throw new InvalidCredentialsException("Authorization header is required");
        }

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
                        post -> ((LocalDateTime) ((Map<String, Object>) post.get("post")).get("createdAt"))))
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
    public void deletePost(UUID postId, UUID userId) {
        Post post = findById(postId);
        if (!post.getPostedBy().equals(userId)) {
            throw new UnauthorizedAccessException("User not authorized to delete this post");
        }
        postRepository.delete(post);
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

    @Override
    public Page<Map<String, Object>> findPostsByDistanceFeed(Double userLat, Double userLon,
            List<String> categories, String keyword, LocalDateTime dateFrom, LocalDateTime dateTo,
            String authorizationHeader, Pageable pageable) throws InvalidCredentialsException {

        UUID userId = jwtService.getUserIdFromAuthorizationHeader(authorizationHeader);

        // Validate categories if provided
        if (categories != null && !categories.isEmpty()) {
            validateCategories(categories);
        }

        // Get all posts
        List<Post> allPosts = postRepository.findAll();

        // Apply filters and calculate distance
        List<Map<String, Object>> postsWithDistance = allPosts.stream()
                .filter(post -> matchesCategories(post, categories))
                .filter(post -> matchesKeyword(post, keyword))
                .filter(post -> matchesDateRange(post, dateFrom, dateTo))
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

        // Use helper method for pagination
        return paginateResults(postsWithDistance, pageable);
    }

    @Override
    public Page<Map<String, Object>> findPostsByTimestampFeed(
            List<String> categories, String keyword, LocalDateTime dateFrom, LocalDateTime dateTo,
            String authorizationHeader, Pageable pageable) throws InvalidCredentialsException {

        UUID userId = jwtService.getUserIdFromAuthorizationHeader(authorizationHeader);

        // Validate categories if provided
        if (categories != null && !categories.isEmpty()) {
            validateCategories(categories);
        }

        // Get all posts
        List<Post> allPosts = postRepository.findAll();

        // Apply filters with AND logic
        List<Map<String, Object>> filteredPosts = allPosts.stream()
                .filter(post -> matchesCategories(post, categories))
                .filter(post -> matchesKeyword(post, keyword))
                .filter(post -> matchesDateRange(post, dateFrom, dateTo))
                .map(post -> {
                    Map<String, Object> result = new HashMap<>();
                    Map<String, Object> postData = mapPostToData(post, userId);
                    result.put("post", postData);
                    return result;
                })
                // Sort by timestamp (earliest first)
                .sorted(Comparator.comparing(
                        post -> ((LocalDateTime) ((Map<String, Object>) post.get("post")).get("createdAt"))))
                .toList();

        return paginateResults(filteredPosts, pageable);
    }

    // Helper methods to reduce cognitive complexity
    private boolean matchesCategories(Post post, List<String> categories) {
        if (categories == null || categories.isEmpty()) {
            return true;
        }
        return post.getCategory() != null && categories.contains(post.getCategory());
    }

    private boolean matchesKeyword(Post post, String keyword) {
        if (keyword == null || keyword.isEmpty()) {
            return true;
        }
        String lowercaseKeyword = keyword.toLowerCase();
        return (post.getTitle() != null && post.getTitle().toLowerCase().contains(lowercaseKeyword)) ||
                (post.getCaption() != null && post.getCaption().toLowerCase().contains(lowercaseKeyword));
    }

    private boolean matchesDateRange(Post post, LocalDateTime dateFrom, LocalDateTime dateTo) {
        LocalDateTime createdAt = post.getCreatedAt();
        boolean matchesFromDate = dateFrom == null || !createdAt.isBefore(dateFrom);
        boolean matchesToDate = dateTo == null || !createdAt.isAfter(dateTo);
        return matchesFromDate && matchesToDate;
    }

    private Page<Map<String, Object>> paginateResults(List<Map<String, Object>> results, Pageable pageable) {
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), results.size());

        List<Map<String, Object>> pageContent = start >= results.size() ? Collections.emptyList()
                : results.subList(start, end);

        return new PageImpl<>(pageContent, pageable, results.size());
    }
}