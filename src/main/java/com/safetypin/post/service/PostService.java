package com.safetypin.post.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import com.safetypin.post.dto.FeedQueryDTO;
import com.safetypin.post.dto.PostCreateRequest;
import com.safetypin.post.dto.PostData;
import com.safetypin.post.dto.PostedByData;
import com.safetypin.post.dto.UserDetails;
import com.safetypin.post.exception.InvalidPostDataException;
import com.safetypin.post.exception.PostException;
import com.safetypin.post.exception.PostNotFoundException;
import com.safetypin.post.exception.UnauthorizedAccessException;
import com.safetypin.post.model.Category;
import com.safetypin.post.model.Post;
import com.safetypin.post.repository.CategoryRepository;
import com.safetypin.post.repository.PostRepository;
import com.safetypin.post.service.strategy.DistanceFeedStrategy;
import com.safetypin.post.service.strategy.FeedStrategy;
import com.safetypin.post.service.strategy.FollowingFeedStrategy;
import com.safetypin.post.service.strategy.TimestampFeedStrategy;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class PostService {

    private final PostRepository postRepository;
    private final CategoryRepository categoryRepository;
    private final DistanceFeedStrategy distanceFeedStrategy;
    private final TimestampFeedStrategy timestampFeedStrategy;
    private final FollowingFeedStrategy followingFeedStrategy;
    private final RestTemplate restTemplate;

    @Value("${be-auth}")
    private String apiEndpoint = "http://safetypin.ppl.cs.ui.ac.id";

    @Autowired
    public PostService(PostRepository postRepository, CategoryRepository categoryRepository,
            DistanceFeedStrategy distanceFeedStrategy,
            TimestampFeedStrategy timestampFeedStrategy,
            FollowingFeedStrategy followingFeedStrategy,
            RestTemplate restTemplate) {
        this.postRepository = postRepository;
        this.categoryRepository = categoryRepository;
        this.distanceFeedStrategy = distanceFeedStrategy;
        this.timestampFeedStrategy = timestampFeedStrategy;
        this.followingFeedStrategy = followingFeedStrategy;
        this.restTemplate = restTemplate;
    }
    // find all (debugging purposes)

    public List<Post> findAll() {
        return postRepository.findAll();
    }

    // find all with pagination

    public Page<Post> findAllPaginated(Pageable pageable) {
        return postRepository.findAll(pageable);
    }

    public Post createPost(PostCreateRequest postCreateRequest) {

        String title = postCreateRequest.getTitle();
        String content = postCreateRequest.getCaption();
        Double latitude = postCreateRequest.getLatitude();
        Double longitude = postCreateRequest.getLongitude();
        String category = postCreateRequest.getCategory();
        UUID postedBy = postCreateRequest.getPostedBy();

        // Get user details from security context
        UserDetails userDetails = getUserDetails();

        // Validate post data
        validatePostData(title, content, latitude, longitude, category, postedBy, userDetails);

        // Create and save the post
        return createAndSavePost(postCreateRequest);
    }

    private UserDetails getUserDetails() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return (UserDetails) authentication.getPrincipal();
    }

    private void validatePostData(String title, String content, Double latitude, Double longitude,
            String category, UUID postedBy, UserDetails userDetails) {
        validateTitleAndContent(title, content, userDetails);
        validateLocation(latitude, longitude);
        validateCategoryAndUser(category, postedBy);
    }

    private void validateTitleAndContent(String title, String content, UserDetails userDetails) {
        int titleLimit = userDetails.getTitleCharacterLimit();
        int captionLimit = userDetails.getCaptionCharacterLimit();
        String userType = userDetails.isPremiumUser() ? "premium" : "free";

        if (title == null || title.trim().isEmpty()) {
            throw new InvalidPostDataException("Title is required");
        }

        if (title.length() > titleLimit) {
            throw new InvalidPostDataException("Title exceeds the character limit of " + titleLimit +
                    " characters for " + userType + " users");
        }

        if (content == null || content.trim().isEmpty()) {
            throw new InvalidPostDataException("Content is required");
        }

        if (content.length() > captionLimit) {
            throw new InvalidPostDataException("Caption exceeds the character limit of " + captionLimit +
                    " characters for " + userType + " users");
        }
    }

    private void validateLocation(Double latitude, Double longitude) {
        if (latitude == null || longitude == null) {
            throw new InvalidPostDataException("Location coordinates are required");
        }
    }

    private void validateCategoryAndUser(String category, UUID postedBy) {
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
    }

    private Post createAndSavePost(PostCreateRequest request) {
        Post post = new Post.Builder()
                .title(request.getTitle())
                .caption(request.getCaption())
                .location(request.getLatitude(), request.getLongitude())
                .category(request.getCategory())
                .postedBy(request.getPostedBy())
                .imageUrl(request.getImageUrl())
                .address(request.getAddress())
                .build();

        try {
            return postRepository.save(post);
        } catch (Exception e) {
            log.error("Error saving post: {}", e.getMessage());
            throw new PostException("Failed to save the post: " + e.getMessage());
        }
    }

    public Post findById(UUID id) {
        return postRepository.findById(id)
                .orElseThrow(() -> new PostNotFoundException("Post not found with id: " + id));
    }

    public void deletePost(UUID postId, UUID userId) {
        Post post = findById(postId);
        if (!post.getPostedBy().equals(userId)) {
            throw new UnauthorizedAccessException("User not authorized to delete this post");
        }
        postRepository.delete(post);
    }

    public Page<Map<String, Object>> getFeed(FeedQueryDTO queryDTO, String feedType) {
        // Validate categories if provided
        if (queryDTO.getCategories() != null && !queryDTO.getCategories().isEmpty()) {
            validateCategories(queryDTO.getCategories());
        }

        // Validate feed type
        if (feedType == null) {
            throw new IllegalArgumentException("Feed type is required");
        }

        FeedStrategy strategy;
        List<Post> allPosts = null; // Will be fetched only if needed by the strategy
        Map<UUID, PostedByData> profileList = null; // Will be fetched only if needed by the strategy

        // Choose strategy based on feed type
        switch (feedType.toLowerCase()) {
            case "distance":
                strategy = distanceFeedStrategy;
                allPosts = postRepository.findAll(); // Distance needs all posts
                // Fetch profiles for all posts
                List<UUID> createdByListDistance = allPosts.stream().map(Post::getPostedBy).distinct().toList();
                profileList = fetchPostedByData(createdByListDistance);
                break;
            case "timestamp":
                strategy = timestampFeedStrategy;
                allPosts = postRepository.findAll(); // Timestamp currently processes all posts
                // Fetch profiles for all posts
                List<UUID> createdByListTimestamp = allPosts.stream().map(Post::getPostedBy).distinct().toList();
                profileList = fetchPostedByData(createdByListTimestamp);
                break;
            case "following": // Added case
                strategy = followingFeedStrategy;
                // Following strategy fetches its own data, no need to fetch allPosts or
                // profileList here
                break;
            default:
                throw new IllegalArgumentException("Invalid feed type: " + feedType);
        }

        // Apply strategy to posts
        // Pass null for allPosts and profileList if the strategy doesn't need them
        // (like FollowingFeedStrategy)
        return strategy.processFeed(allPosts, queryDTO, profileList);
    }

    public Page<Map<String, Object>> findPostsByUser(UUID postUserId, Pageable pageable) {
        if (postUserId == null) {
            throw new IllegalArgumentException("Post user ID is required");
        }

        // Get all posts with filters and ordered
        Page<Post> allPosts = postRepository.findByPostedByOrderByCreatedAtDesc(postUserId, pageable);

        // collect createdBy UUID from allPosts
        List<UUID> createdByList = allPosts.stream()
                .map(Post::getPostedBy)
                .distinct()
                .toList();
        // fetch profiles
        Map<UUID, PostedByData> profileList = fetchPostedByData(createdByList);

        // Map to PostData and return page
        return allPosts.map(post -> {
            Map<String, Object> result = new HashMap<>();
            PostData postData = PostData.fromPostAndUserId(post, postUserId, (profileList.get(post.getPostedBy())));
            result.put("post", postData);
            return result;
        });
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

    public Map<UUID, PostedByData> fetchPostedByData(List<UUID> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return new HashMap<>();
        }
        // fetch profiles
        String uri = apiEndpoint + "/api/profiles/batch"; // or any other uri
        HttpEntity<List<UUID>> entity = new HttpEntity<>(userIds, null);
        try {
            ResponseEntity<Map<UUID, PostedByData>> result = restTemplate.exchange(uri, HttpMethod.POST, entity,
                    new ParameterizedTypeReference<Map<UUID, PostedByData>>() {
                    });

            log.info("Fetched {} profiles successfully.", result.getBody() != null ? result.getBody().size() : 0);
            return result.getBody() == null ? new HashMap<>() : result.getBody();
        } catch (ResourceAccessException e) {
            log.error("Network error fetching profiles for user IDs {}: {}", userIds, e.getMessage());
            // Return empty map on network error
            return new HashMap<>();
        } catch (Exception e) {
            log.error("Error fetching profiles for user IDs {}: {}", userIds, e.getMessage(), e);
            // Return empty map on other errors during fetch
            return new HashMap<>();
        }
    }
}