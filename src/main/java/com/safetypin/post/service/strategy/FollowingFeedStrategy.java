package com.safetypin.post.service.strategy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import com.safetypin.post.dto.ApiResponse;
import com.safetypin.post.dto.FeedQueryDTO;
import com.safetypin.post.dto.PostData;
import com.safetypin.post.dto.PostedByData;
import com.safetypin.post.dto.UserFollowResponse;
import com.safetypin.post.model.Post;
import com.safetypin.post.repository.PostRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class FollowingFeedStrategy extends AbstractFeedStrategy {

    private final PostRepository postRepository;
    private final RestTemplate restTemplate;

    @Value("${be-auth}")
    private String apiEndpoint = "http://safetypin.ppl.cs.ui.ac.id"; // Default value

    @Autowired
    public FollowingFeedStrategy(PostRepository postRepository, RestTemplate restTemplate) {
        this.postRepository = postRepository;
        this.restTemplate = restTemplate;
    }

    private Map<UUID, PostedByData> fetchFollowingUsers(UUID userId) {
        String uri = apiEndpoint + "/api/follow/following/" + userId;
        try {
            // Get the JWT token from the SecurityContext
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String jwt = (String) authentication.getCredentials();

            // Create headers with Authorization
            HttpHeaders headers = new HttpHeaders();
            if (jwt != null && !jwt.isEmpty()) {
                headers.set("Authorization", "Bearer " + jwt);
            } else {
                log.warn("No JWT token found in security context when fetching following users for userId: {}", userId);
            }

            // Create the HTTP entity with headers
            HttpEntity<?> entity = new HttpEntity<>(headers);

            // Create a generic type to match the API response wrapper structure
            ParameterizedTypeReference<ApiResponse<List<UserFollowResponse>>> responseType = new ParameterizedTypeReference<ApiResponse<List<UserFollowResponse>>>() {
            };

            // Make the request with headers
            ResponseEntity<ApiResponse<List<UserFollowResponse>>> response = restTemplate.exchange(
                    uri,
                    HttpMethod.GET,
                    entity,
                    responseType);

            ApiResponse<List<UserFollowResponse>> apiResponse = response.getBody();
            if (apiResponse != null && apiResponse.getData() != null) {
                // Convert List<UserFollowResponse> to Map<UUID, PostedByData>
                Map<UUID, PostedByData> result = new HashMap<>();
                for (UserFollowResponse user : apiResponse.getData()) {
                    if (user != null && user.getUserId() != null) {
                        // Map UserFollowResponse fields to PostedByData fields
                        PostedByData postedByData = new PostedByData(
                                user.getUserId(),
                                user.getName(),
                                user.getProfilePicture());
                        result.put(user.getUserId(), postedByData);
                    }
                }
                return result;
            } else {
                log.warn("Received null body or null data when fetching following users for userId: {}", userId);
                return Collections.emptyMap();
            }
        } catch (ResourceAccessException e) {
            log.error("Network error fetching following list for user ID {}: {}", userId, e.getMessage());
            // Return empty map on network error
            return Collections.emptyMap();
        } catch (Exception e) {
            log.error("Error fetching following list for user ID {}: {}", userId, e.getMessage(), e);
            // Return empty map on other errors
            return Collections.emptyMap();
        }
    }

    @Override
    public Page<Map<String, Object>> processFeed(List<Post> allPostsIgnored, FeedQueryDTO queryDTO,
            Map<UUID, PostedByData> profileListIgnored) {
        // 1. Fetch the users the current user is following
        Map<UUID, PostedByData> followingUsersMap = fetchFollowingUsers(queryDTO.getUserId());
        List<UUID> followingUserIds = new ArrayList<>(followingUsersMap.keySet());

        if (followingUserIds.isEmpty()) {
            log.info("User {} is not following anyone. Returning empty feed.", queryDTO.getUserId());
            return Page.empty(queryDTO.getPageable());
        }

        // 2. Fetch posts made by the followed users
        List<Post> postsByFollowedUsers = postRepository.findByPostedByIn(followingUserIds);

        // 3. Apply filters (category, keyword, date range) and sort
        List<Map<String, Object>> filteredAndSortedPosts = postsByFollowedUsers.stream()
                .filter(post -> matchesCategories(post, queryDTO.getCategories()))
                .filter(post -> matchesKeyword(post, queryDTO.getKeyword()))
                .filter(post -> matchesDateRange(post, queryDTO.getDateFrom(), queryDTO.getDateTo()))
                .sorted(Comparator.comparing(Post::getCreatedAt).reversed()) // Sort by newest first
                .map(post -> {
                    Map<String, Object> result = new HashMap<>();
                    // Use the profile data fetched from the following API
                    PostedByData authorData = followingUsersMap.get(post.getPostedBy());
                    PostData postData = PostData.fromPostAndUserId(post, queryDTO.getUserId(), authorData);
                    result.put("post", postData);
                    return result;
                })
                .toList();

        // 4. Paginate the results
        return paginateResults(filteredAndSortedPosts, queryDTO.getPageable());
    }
}
