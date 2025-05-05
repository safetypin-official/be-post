package com.safetypin.post.service.strategy;

import com.safetypin.post.dto.*;
import com.safetypin.post.model.Post;
import com.safetypin.post.repository.PostRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FollowingFeedStrategyTest {

    private static final String TEST_JWT = "test-jwt-token";
    // Define the specific ParameterizedTypeReference for mocking
    private static final ParameterizedTypeReference<ApiResponse<List<UserFollowResponse>>> API_RESPONSE_TYPE_REF = new ParameterizedTypeReference<>() {
    };
    private final String apiEndpoint = "http://test-endpoint";
    @Mock
    private PostRepository postRepository;
    @Mock
    private RestTemplate restTemplate;
    @Mock
    private SecurityContext securityContext;
    @Mock
    private Authentication authentication;
    @InjectMocks
    private FollowingFeedStrategy followingFeedStrategy;
    private UUID userId;
    private UUID followedUserId1;
    private UUID followedUserId2;
    private PostedByData followedUser1Data;
    private PostedByData followedUser2Data;
    private UserFollowResponse followedUser1Response;
    private UserFollowResponse followedUser2Response;
    private Post post1;
    private Post post2;
    private Post post3; // Post by a non-followed user or filtered out
    private FeedQueryDTO queryDTO;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        followedUserId1 = UUID.randomUUID();
        followedUserId2 = UUID.randomUUID();

        // Mock security context
        when(authentication.getCredentials()).thenReturn(TEST_JWT);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        // Create both UserFollowResponse and PostedByData objects for test data
        // preparation
        followedUser1Response = new UserFollowResponse(followedUserId1, "followedUser1Name", "pic1", true);
        followedUser2Response = new UserFollowResponse(followedUserId2, "followedUser2Name", "pic2", true);

        followedUser1Data = new PostedByData(followedUserId1, "followedUser1Name", "pic1");
        followedUser2Data = new PostedByData(followedUserId2, "followedUser2Name", "pic2");

        // Use Post Builder and correct field names/types
        post1 = Post.builder()
                .id(UUID.randomUUID())
                .caption("Content 1")
                .imageUrl("image1.jpg")
                .postedBy(followedUserId1)
                .createdAt(LocalDateTime.now().minusDays(1))
                .category("CAT1") // Assuming category is String based on Post.java
                .latitude(1.0)
                .longitude(1.0)
                .build();

        post2 = Post.builder()
                .id(UUID.randomUUID())
                .caption("Content 2 keyword")
                .imageUrl("image2.jpg")
                .postedBy(followedUserId2)
                .createdAt(LocalDateTime.now())
                .category("CAT2")
                .latitude(2.0)
                .longitude(2.0)
                .build();

        post3 = Post.builder()
                .id(UUID.randomUUID())
                .caption("Content 3 old")
                .imageUrl("image3.jpg")
                .postedBy(followedUserId1) // Belongs to followed user, but might be filtered
                .createdAt(LocalDateTime.now().minusDays(5))
                .category("CAT1")
                .latitude(3.0)
                .longitude(3.0)
                .build(); // Older post

        pageable = PageRequest.of(0, 10);
        // Use FeedQueryDTO Builder
        queryDTO = FeedQueryDTO.builder()
                .userId(userId)
                .pageable(pageable)
                .build();

        // Set the private field value using ReflectionTestUtils
        ReflectionTestUtils.setField(followingFeedStrategy, "apiEndpoint", apiEndpoint);
    }

    private ResponseEntity<ApiResponse<List<UserFollowResponse>>> createFollowingResponse(
            List<UserFollowResponse> body) {
        ApiResponse<List<UserFollowResponse>> apiResponse = ApiResponse.<List<UserFollowResponse>>builder()
                .status("success")
                .data(body)
                .message("Following list retrieved successfully")
                .build();
        return new ResponseEntity<>(apiResponse, HttpStatus.OK);
    }


    @Test
    void processFeed_Success_ReturnsPaginatedPosts() {
        // Arrange
        List<UserFollowResponse> followingList = Arrays.asList(followedUser1Response, followedUser2Response);
        ResponseEntity<ApiResponse<List<UserFollowResponse>>> responseEntity = createFollowingResponse(
                followingList);
        String expectedUri = apiEndpoint + "/api/follow/following/" + userId;

        when(restTemplate.exchange(eq(expectedUri), eq(HttpMethod.GET), any(HttpEntity.class),
                eq(API_RESPONSE_TYPE_REF)))
                .thenReturn(responseEntity);

        List<UUID> expectedFollowedIds = new ArrayList<>(Arrays.asList(followedUserId1, followedUserId2));
        List<Post> posts = Arrays.asList(post1, post2);

        // Use argThat with a HashSet comparison for order-insensitive matching
        when(postRepository
                .findByPostedByIn(argThat(list -> new HashSet<>(list)
                        .equals(new HashSet<>(expectedFollowedIds)))))
                .thenReturn(posts);

        // Act
        Page<Map<String, Object>> result = followingFeedStrategy.processFeed(null, queryDTO, null);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
        assertEquals(1, result.getTotalPages());
        assertEquals(2, result.getContent().size());

        assertEquals(post2.getId(), ((PostData) result.getContent().get(0).get("post")).getId());
        assertEquals(followedUser2Data.getName(),
                ((PostData) result.getContent().get(0).get("post")).getPostedBy().getName());
        assertEquals(post1.getId(), ((PostData) result.getContent().get(1).get("post")).getId());
        assertEquals(followedUser1Data.getName(),
                ((PostData) result.getContent().get(1).get("post")).getPostedBy().getName());

        verify(restTemplate).exchange(eq(expectedUri), eq(HttpMethod.GET), any(HttpEntity.class),
                eq(API_RESPONSE_TYPE_REF));
        // Use argThat for verification as well
        verify(postRepository)
                .findByPostedByIn(argThat(list -> new HashSet<>(list)
                        .equals(new HashSet<>(expectedFollowedIds))));
    }

    @Test
    void processFeed_Success_WithFiltersAndPagination() {
        // Arrange
        // Use FeedQueryDTO Builder and correct argument types (List<String> for
        // categories)
        queryDTO = FeedQueryDTO.builder()
                .userId(userId)
                .keyword("keyword")
                .categories(List.of("CAT2")) // Use List<String>
                .dateFrom(LocalDateTime.now().minusDays(2))
                .dateTo(LocalDateTime.now().plusDays(1))
                .pageable(PageRequest.of(0, 1))
                .build();

        List<UserFollowResponse> followingList = Arrays.asList(followedUser1Response, followedUser2Response);
        ResponseEntity<ApiResponse<List<UserFollowResponse>>> responseEntity = createFollowingResponse(
                followingList);
        String expectedUri = apiEndpoint + "/api/follow/following/" + userId;

        when(restTemplate.exchange(eq(expectedUri), eq(HttpMethod.GET), any(HttpEntity.class),
                eq(API_RESPONSE_TYPE_REF)))
                .thenReturn(responseEntity);

        List<UUID> followedIds = new ArrayList<>(Arrays.asList(followedUserId1, followedUserId2));
        List<Post> postsFromRepo = Arrays.asList(post1, post2, post3);

        // Use argThat with HashSet comparison
        when(postRepository.findByPostedByIn(
                argThat(list -> new HashSet<>(list).equals(new HashSet<>(followedIds)))))
                .thenReturn(postsFromRepo);

        // Act
        Page<Map<String, Object>> result = followingFeedStrategy.processFeed(null, queryDTO, null);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements()); // Only post2 matches all filters
        assertEquals(1, result.getTotalPages());
        assertEquals(1, result.getContent().size()); // Page size is 1

        // Use correct getters
        assertEquals(post2.getId(), ((PostData) result.getContent().getFirst().get("post")).getId());
        assertEquals(followedUser2Data.getName(),
                ((PostData) result.getContent().getFirst().get("post")).getPostedBy().getName());

        verify(restTemplate).exchange(eq(expectedUri), eq(HttpMethod.GET), any(HttpEntity.class),
                eq(API_RESPONSE_TYPE_REF));
        // Use argThat for verification
        verify(postRepository)
                .findByPostedByIn(argThat(
                        list -> new HashSet<>(list).equals(new HashSet<>(followedIds))));
    }

    @Test
    void processFeed_UserNotFollowingAnyone() {
        // Arrange
        ResponseEntity<ApiResponse<List<UserFollowResponse>>> responseEntity = createFollowingResponse(
                Collections.emptyList());
        String expectedUri = apiEndpoint + "/api/follow/following/" + userId;

        when(restTemplate.exchange(eq(expectedUri), eq(HttpMethod.GET), any(HttpEntity.class),
                eq(API_RESPONSE_TYPE_REF)))
                .thenReturn(responseEntity);

        // Act
        Page<Map<String, Object>> result = followingFeedStrategy.processFeed(null, queryDTO, null);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        assertEquals(0, result.getTotalElements());

        verify(restTemplate).exchange(eq(expectedUri), eq(HttpMethod.GET), any(HttpEntity.class),
                eq(API_RESPONSE_TYPE_REF));
        verify(postRepository, never()).findByPostedByIn(anyList());
    }

    @Test
    void processFeed_FetchFollowing_ReturnsNullBody() {
        // Arrange
        // Create ApiResponse with null data
        ApiResponse<List<UserFollowResponse>> nullDataResponse = ApiResponse.<List<UserFollowResponse>>builder()
                .status("success")
                .data(null)
                .message("No data found")
                .build();

        ResponseEntity<ApiResponse<List<UserFollowResponse>>> responseEntity = new ResponseEntity<>(
                nullDataResponse, HttpStatus.OK);

        String expectedUri = apiEndpoint + "/api/follow/following/" + userId;

        when(restTemplate.exchange(eq(expectedUri), eq(HttpMethod.GET), any(HttpEntity.class),
                eq(API_RESPONSE_TYPE_REF)))
                .thenReturn(responseEntity);

        // Act
        Page<Map<String, Object>> result = followingFeedStrategy.processFeed(null, queryDTO, null);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        assertEquals(0, result.getTotalElements());

        verify(restTemplate).exchange(eq(expectedUri), eq(HttpMethod.GET), any(HttpEntity.class),
                eq(API_RESPONSE_TYPE_REF));
        verify(postRepository, never()).findByPostedByIn(anyList());
    }

    @Test
    void processFeed_FetchFollowing_ReturnsDataWithNullUserId() {
        // Arrange
        // Simulate a case where the API returns data but some entries are malformed
        UserFollowResponse malformedResponse = new UserFollowResponse(null, "malformedUserName",
                "pic_malformed", true);
        List<UserFollowResponse> followingList = Arrays.asList(followedUser1Response, malformedResponse);
        ResponseEntity<ApiResponse<List<UserFollowResponse>>> responseEntity = createFollowingResponse(
                followingList);
        String expectedUri = apiEndpoint + "/api/follow/following/" + userId;

        when(restTemplate.exchange(eq(expectedUri), eq(HttpMethod.GET), any(HttpEntity.class),
                eq(API_RESPONSE_TYPE_REF)))
                .thenReturn(responseEntity);

        // Only followedUserId1 should be queried in the repo
        List<UUID> validFollowedIds = Collections.singletonList(followedUserId1);
        List<Post> posts = Collections.singletonList(post1);
        // No need for argThat here as it's a singleton list, order doesn't matter
        when(postRepository.findByPostedByIn(eq(validFollowedIds))).thenReturn(posts);

        // Act
        Page<Map<String, Object>> result = followingFeedStrategy.processFeed(null, queryDTO, null);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements()); // Only post from followedUser1 should be present
        assertEquals(1, result.getContent().size());
        // Use correct getters
        assertEquals(post1.getId(), ((PostData) result.getContent().getFirst().get("post")).getId());
        assertEquals(followedUser1Data.getName(),
                ((PostData) result.getContent().getFirst().get("post")).getPostedBy().getName());

        verify(restTemplate).exchange(eq(expectedUri), eq(HttpMethod.GET), any(HttpEntity.class),
                eq(API_RESPONSE_TYPE_REF));
        // Verification for singleton list
        verify(postRepository).findByPostedByIn(eq(validFollowedIds));
    }

    @Test
    void processFeed_FetchFollowing_NetworkError() {
        // Arrange
        String expectedUri = apiEndpoint + "/api/follow/following/" + userId;
        when(restTemplate.exchange(eq(expectedUri), eq(HttpMethod.GET), any(HttpEntity.class),
                eq(API_RESPONSE_TYPE_REF)))
                .thenThrow(new ResourceAccessException("Network error"));

        // Act
        Page<Map<String, Object>> result = followingFeedStrategy.processFeed(null, queryDTO, null);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        assertEquals(0, result.getTotalElements());

        verify(restTemplate).exchange(eq(expectedUri), eq(HttpMethod.GET), any(HttpEntity.class),
                eq(API_RESPONSE_TYPE_REF));
        verify(postRepository, never()).findByPostedByIn(anyList());
    }

    @Test
    void processFeed_FetchFollowing_OtherException() {
        // Arrange
        String expectedUri = apiEndpoint + "/api/follow/following/" + userId;
        when(restTemplate.exchange(eq(expectedUri), eq(HttpMethod.GET), any(HttpEntity.class),
                eq(API_RESPONSE_TYPE_REF)))
                .thenThrow(new RuntimeException("Some other error"));

        // Act
        Page<Map<String, Object>> result = followingFeedStrategy.processFeed(null, queryDTO, null);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        assertEquals(0, result.getTotalElements());

        verify(restTemplate).exchange(eq(expectedUri), eq(HttpMethod.GET), any(HttpEntity.class),
                eq(API_RESPONSE_TYPE_REF));
        verify(postRepository, never()).findByPostedByIn(anyList());
    }

    @Test
    void processFeed_NoPostsFoundForFollowedUsers() {
        // Arrange
        List<UserFollowResponse> followingList = Arrays.asList(followedUser1Response, followedUser2Response);
        ResponseEntity<ApiResponse<List<UserFollowResponse>>> responseEntity = createFollowingResponse(
                followingList);
        String expectedUri = apiEndpoint + "/api/follow/following/" + userId;

        when(restTemplate.exchange(eq(expectedUri), eq(HttpMethod.GET), any(HttpEntity.class),
                eq(API_RESPONSE_TYPE_REF)))
                .thenReturn(responseEntity);

        List<UUID> expectedFollowedIds = new ArrayList<>(Arrays.asList(followedUserId1, followedUserId2));

        // Use argThat with HashSet comparison
        when(postRepository
                .findByPostedByIn(argThat(list -> new HashSet<>(list)
                        .equals(new HashSet<>(expectedFollowedIds)))))
                .thenReturn(Collections.emptyList()); // No posts found

        // Act
        Page<Map<String, Object>> result = followingFeedStrategy.processFeed(null, queryDTO, null);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        assertEquals(0, result.getTotalElements());

        verify(restTemplate).exchange(eq(expectedUri), eq(HttpMethod.GET), any(HttpEntity.class),
                eq(API_RESPONSE_TYPE_REF));
        // Use argThat for verification
        verify(postRepository)
                .findByPostedByIn(argThat(list -> new HashSet<>(list)
                        .equals(new HashSet<>(expectedFollowedIds))));
    }

    @Test
    void processFeed_FilteringResultsInEmptyList() {
        // Arrange
        // Use filters that won't match any posts
        // Use FeedQueryDTO Builder and correct argument types (List<String> for
        // categories)
        queryDTO = FeedQueryDTO.builder()
                .userId(userId)
                .keyword("nonexistent_keyword")
                .categories(List.of("NONEXISTENT_CAT")) // Use List<String>
                .pageable(pageable)
                .build();

        List<UserFollowResponse> followingList = Arrays.asList(followedUser1Response, followedUser2Response);
        ResponseEntity<ApiResponse<List<UserFollowResponse>>> responseEntity = createFollowingResponse(
                followingList);
        String expectedUri = apiEndpoint + "/api/follow/following/" + userId;

        when(restTemplate.exchange(eq(expectedUri), eq(HttpMethod.GET), any(HttpEntity.class),
                eq(API_RESPONSE_TYPE_REF)))
                .thenReturn(responseEntity);

        List<UUID> followedIds = new ArrayList<>(Arrays.asList(followedUserId1, followedUserId2));
        List<Post> posts = Arrays.asList(post1, post2); // These posts exist but won't match filters

        // Use argThat with HashSet comparison
        when(postRepository.findByPostedByIn(
                argThat(list -> new HashSet<>(list).equals(new HashSet<>(followedIds)))))
                .thenReturn(posts);

        // Act
        Page<Map<String, Object>> result = followingFeedStrategy.processFeed(null, queryDTO, null);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        assertEquals(0, result.getTotalElements()); // 0 because filtering removed all posts

        verify(restTemplate).exchange(eq(expectedUri), eq(HttpMethod.GET), any(HttpEntity.class),
                eq(API_RESPONSE_TYPE_REF));
        // Use argThat for verification
        verify(postRepository)
                .findByPostedByIn(argThat(
                        list -> new HashSet<>(list).equals(new HashSet<>(followedIds))));
    }
}
