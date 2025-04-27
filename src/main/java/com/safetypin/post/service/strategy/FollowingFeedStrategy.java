package com.safetypin.post.service.strategy;

import com.safetypin.post.dto.FeedQueryDTO;
import com.safetypin.post.dto.PostData;
import com.safetypin.post.dto.PostedByData;
import com.safetypin.post.model.Post;
import com.safetypin.post.repository.PostRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.*;

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
            ResponseEntity<List<PostedByData>> response = restTemplate.exchange(uri, HttpMethod.GET, null,
                    new ParameterizedTypeReference<List<PostedByData>>() {
                    });

            List<PostedByData> responseBody = response.getBody();
            if (responseBody != null) {
                // Convert List<PostedByData> to Map<UUID, PostedByData> using explicit lambda
                // expressions
                Map<UUID, PostedByData> result = new HashMap<>();
                for (PostedByData data : responseBody) {
                    if (data != null && data.getUserId() != null) {
                        result.put(data.getUserId(), data);
                    }
                }
                return result;
            } else {
                log.warn("Received null body when fetching following users for userId: {}", userId);
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
