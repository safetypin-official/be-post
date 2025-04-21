package com.safetypin.post.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.safetypin.post.dto.AuthResponse;
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
import com.safetypin.post.service.strategy.TimestampFeedStrategy;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class PostServiceImpl implements PostService {

    private final PostRepository postRepository;
    private final CategoryRepository categoryRepository;
    private final DistanceFeedStrategy distanceFeedStrategy;
    private final TimestampFeedStrategy timestampFeedStrategy;
    // @Value("${be-auth}")
    private final String apiEndpoint = "https://safetypin.ppl.cs.ui.ac.id";

    @Autowired
    public PostServiceImpl(PostRepository postRepository, CategoryRepository categoryRepository,
            DistanceFeedStrategy distanceFeedStrategy,
            TimestampFeedStrategy timestampFeedStrategy) {
        this.postRepository = postRepository;
        this.categoryRepository = categoryRepository;
        this.distanceFeedStrategy = distanceFeedStrategy;
        this.timestampFeedStrategy = timestampFeedStrategy;
    }

    private static List<LinkedHashMap<String, String>> getLinkedHashMaps(ResponseEntity<AuthResponse> result) {
        Object data = Objects.requireNonNull(result.getBody()).getData();
        List<?> rawList = (List<?>) data;
        List<LinkedHashMap<String, String>> linkedHashMapList = new ArrayList<>();

        for (Object obj : rawList) {
            if (obj instanceof LinkedHashMap<?, ?> map) {
                LinkedHashMap<String, String> stringMap = new LinkedHashMap<>();
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    stringMap.put(
                            String.valueOf(entry.getKey()),
                            String.valueOf(entry.getValue()) // force String conversion
                    );
                }
                linkedHashMapList.add(stringMap);
            }
        }
        return linkedHashMapList;
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

    @Override
    public Post createPost(PostCreateRequest postCreateRequest) {

        String title = postCreateRequest.getTitle();
        String content = postCreateRequest.getCaption();
        Double latitude = postCreateRequest.getLatitude();
        Double longitude = postCreateRequest.getLongitude();
        String category = postCreateRequest.getCategory();
        UUID postedBy = postCreateRequest.getPostedBy();
        String imageUrl = postCreateRequest.getImageUrl();
        String address = postCreateRequest.getAddress();

        // Get user details from security context to check role-based limits
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        // Apply character limits based on user role
        int titleLimit = userDetails.getTitleCharacterLimit();
        int captionLimit = userDetails.getCaptionCharacterLimit();

        if (title == null || title.trim().isEmpty()) {
            throw new InvalidPostDataException("Title is required");
        }

        if (title.length() > titleLimit) {
            throw new InvalidPostDataException("Title exceeds the character limit of " + titleLimit +
                    " characters for " + (userDetails.isPremiumUser() ? "premium" : "free") + " users");
        }

        if (content == null || content.trim().isEmpty()) {
            throw new InvalidPostDataException("Content is required");
        }

        if (content.length() > captionLimit) {
            throw new InvalidPostDataException("Caption exceeds the character limit of " + captionLimit +
                    " characters for " + (userDetails.isPremiumUser() ? "premium" : "free") + " users");
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
                .imageUrl(imageUrl)
                .address(address)
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

    @Override
    public Page<Map<String, Object>> getFeed(FeedQueryDTO queryDTO, String feedType) {
        // Validate categories if provided
        if (queryDTO.getCategories() != null && !queryDTO.getCategories().isEmpty()) {
            validateCategories(queryDTO.getCategories());
        }

        // Validate feed type
        if (feedType == null) {
            throw new IllegalArgumentException("Feed type is required");
        }

        // Choose strategy based on feed type
        FeedStrategy strategy = switch (feedType.toLowerCase()) {
            case "distance" -> distanceFeedStrategy;
            case "timestamp" -> timestampFeedStrategy;
            default -> throw new IllegalArgumentException("Invalid feed type: " + feedType);
        };

        // Get all posts
        List<Post> allPosts = postRepository.findAll();

        // fetch profiles
        List<PostedByData> profileList = fetchProfiles();

        // Apply strategy to posts
        return strategy.processFeed(allPosts, queryDTO, profileList);
    }

    @Override
    public Page<Map<String, Object>> findPostsByUser(UUID postUserId, Pageable pageable) {
        if (postUserId == null) {
            throw new IllegalArgumentException("Post user ID is required");
        }

        // Get all posts with filters and ordered
        Page<Post> allPosts = postRepository.findByPostedByOrderByCreatedAtDesc(postUserId, pageable);

        // fetch profiles
        List<PostedByData> profileList = fetchProfiles();

        // Map to PostData and return page
        return allPosts.map(post -> {
            Map<String, Object> result = new HashMap<>();
            PostData postData = PostData.fromPostAndUserId(post, postUserId, profileList);
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

    @Override
    public List<PostedByData> fetchProfiles() {
        // fetch profiles
        RestTemplate restTemplate = new RestTemplate();
        String uri = apiEndpoint + "/api/profiles"; // or any other uri
        HttpEntity<String> entity = new HttpEntity<>(null, null);
        ResponseEntity<AuthResponse> result = restTemplate.exchange(uri, HttpMethod.GET, entity, AuthResponse.class);

        try {
            List<LinkedHashMap<String, String>> linkedHashMapList = getLinkedHashMaps(result);

            List<PostedByData> output = new ArrayList<>();
            for (LinkedHashMap<String, String> map : linkedHashMapList) {
                PostedByData postedByData = PostedByData.builder()
                        .name(map.get("name"))
                        .id(UUID.fromString(map.get("id")))
                        .profilePicture(map.get("profilePicture"))
                        .build();
                output.add(postedByData);
            }
            return output;
        } catch (ClassCastException e) {
            throw new InvalidPostDataException("Failed parsing profiles");
        }

    }
}