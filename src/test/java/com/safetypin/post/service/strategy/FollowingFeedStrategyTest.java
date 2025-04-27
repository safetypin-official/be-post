package com.safetypin.post.service.strategy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat; // Ensure argThat is imported
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet; // Import HashSet
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import com.safetypin.post.dto.FeedQueryDTO;
import com.safetypin.post.dto.PostData;
import com.safetypin.post.dto.PostedByData;
import com.safetypin.post.model.Post;
import com.safetypin.post.repository.PostRepository;

@ExtendWith(MockitoExtension.class)
class FollowingFeedStrategyTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private FollowingFeedStrategy followingFeedStrategy;

    private UUID userId;
    private UUID followedUserId1;
    private UUID followedUserId2;
    private PostedByData followedUser1Data;
    private PostedByData followedUser2Data;
    private Post post1;
    private Post post2;
    private Post post3; // Post by a non-followed user or filtered out
    private FeedQueryDTO queryDTO;
    private Pageable pageable;
    private String apiEndpoint = "http://test-endpoint";

    // Define the specific ParameterizedTypeReference for mocking
    private static final ParameterizedTypeReference<List<PostedByData>> LIST_POSTEDBYDATA_TYPE_REF = new ParameterizedTypeReference<List<PostedByData>>() {
    };

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        followedUserId1 = UUID.randomUUID();
        followedUserId2 = UUID.randomUUID();

        // Use correct field names from PostedByData
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

    private ResponseEntity<List<PostedByData>> createFollowingResponse(List<PostedByData> body) {
        // Use HttpStatus.OK directly for non-null body
        return new ResponseEntity<>(body, HttpStatus.OK);
    }

    @Test
    void processFeed_Success_ReturnsPaginatedPosts() {
        // Arrange
        List<PostedByData> followingList = Arrays.asList(followedUser1Data, followedUser2Data);
        ResponseEntity<List<PostedByData>> responseEntity = createFollowingResponse(followingList);
        String expectedUri = apiEndpoint + "/api/follow/following/" + userId;

        when(restTemplate.exchange(eq(expectedUri), eq(HttpMethod.GET), isNull(), eq(LIST_POSTEDBYDATA_TYPE_REF)))
                .thenReturn(responseEntity);

        List<UUID> expectedFollowedIds = new ArrayList<>(Arrays.asList(followedUserId1, followedUserId2));
        List<Post> posts = Arrays.asList(post1, post2);

        // Use argThat with a HashSet comparison for order-insensitive matching
        when(postRepository
                .findByPostedByIn(argThat(list -> new HashSet<>(list).equals(new HashSet<>(expectedFollowedIds)))))
                .thenReturn(posts);

        // Act
        Page<Map<String, Object>> result = followingFeedStrategy.processFeed(null, queryDTO, null);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.getTotalElements()); // Failing assertion
        assertEquals(1, result.getTotalPages());
        assertEquals(2, result.getContent().size());

        assertEquals(post2.getId(), ((PostData) result.getContent().get(0).get("post")).getId());
        assertEquals(followedUser2Data.getName(),
                ((PostData) result.getContent().get(0).get("post")).getPostedBy().getName());
        assertEquals(post1.getId(), ((PostData) result.getContent().get(1).get("post")).getId());
        assertEquals(followedUser1Data.getName(),
                ((PostData) result.getContent().get(1).get("post")).getPostedBy().getName());

        verify(restTemplate).exchange(eq(expectedUri), eq(HttpMethod.GET), isNull(), eq(LIST_POSTEDBYDATA_TYPE_REF));
        // Use argThat for verification as well
        verify(postRepository)
                .findByPostedByIn(argThat(list -> new HashSet<>(list).equals(new HashSet<>(expectedFollowedIds))));
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

        List<PostedByData> followingList = Arrays.asList(followedUser1Data, followedUser2Data);
        ResponseEntity<List<PostedByData>> responseEntity = createFollowingResponse(followingList);
        String expectedUri = apiEndpoint + "/api/follow/following/" + userId;

        when(restTemplate.exchange(eq(expectedUri), eq(HttpMethod.GET), isNull(), eq(LIST_POSTEDBYDATA_TYPE_REF)))
                .thenReturn(responseEntity);

        List<UUID> followedIds = new ArrayList<>(Arrays.asList(followedUserId1, followedUserId2));
        List<Post> postsFromRepo = Arrays.asList(post1, post2, post3);

        // Use argThat with HashSet comparison
        when(postRepository.findByPostedByIn(argThat(list -> new HashSet<>(list).equals(new HashSet<>(followedIds)))))
                .thenReturn(postsFromRepo);

        // Act
        Page<Map<String, Object>> result = followingFeedStrategy.processFeed(null, queryDTO, null);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements()); // Only post2 matches all filters
        assertEquals(1, result.getTotalPages());
        assertEquals(1, result.getContent().size()); // Page size is 1

        // Use correct getters
        assertEquals(post2.getId(), ((PostData) result.getContent().get(0).get("post")).getId());
        assertEquals(followedUser2Data.getName(),
                ((PostData) result.getContent().get(0).get("post")).getPostedBy().getName());

        verify(restTemplate).exchange(eq(expectedUri), eq(HttpMethod.GET), isNull(), eq(LIST_POSTEDBYDATA_TYPE_REF));
        // Use argThat for verification
        verify(postRepository)
                .findByPostedByIn(argThat(list -> new HashSet<>(list).equals(new HashSet<>(followedIds))));
    }

    @Test
    void processFeed_UserNotFollowingAnyone() {
        // Arrange
        ResponseEntity<List<PostedByData>> responseEntity = createFollowingResponse(Collections.emptyList());
        String expectedUri = apiEndpoint + "/api/follow/following/" + userId;

        when(restTemplate.exchange(eq(expectedUri), eq(HttpMethod.GET), isNull(), eq(LIST_POSTEDBYDATA_TYPE_REF)))
                .thenReturn(responseEntity);

        // Act
        Page<Map<String, Object>> result = followingFeedStrategy.processFeed(null, queryDTO, null);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        assertEquals(0, result.getTotalElements());

        verify(restTemplate).exchange(eq(expectedUri), eq(HttpMethod.GET), isNull(), eq(LIST_POSTEDBYDATA_TYPE_REF));
        verify(postRepository, never()).findByPostedByIn(anyList());
    }

    @Test
    void processFeed_FetchFollowing_ReturnsNullBody() {
        // Arrange
        // Pass null directly to ResponseEntity constructor for null body case
        // Provide empty headers to satisfy constructor requirements
        ResponseEntity<List<PostedByData>> responseEntity = new ResponseEntity<>(null,
                new org.springframework.util.LinkedMultiValueMap<>(), HttpStatus.OK);
        String expectedUri = apiEndpoint + "/api/follow/following/" + userId;

        when(restTemplate.exchange(eq(expectedUri), eq(HttpMethod.GET), isNull(), eq(LIST_POSTEDBYDATA_TYPE_REF)))
                .thenReturn(responseEntity);

        // Act
        Page<Map<String, Object>> result = followingFeedStrategy.processFeed(null, queryDTO, null);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        assertEquals(0, result.getTotalElements());

        verify(restTemplate).exchange(eq(expectedUri), eq(HttpMethod.GET), isNull(), eq(LIST_POSTEDBYDATA_TYPE_REF));
        verify(postRepository, never()).findByPostedByIn(anyList());
    }

    @Test
    void processFeed_FetchFollowing_ReturnsDataWithNullUserId() {
        // Arrange
        // Simulate a case where the API returns data but some entries are malformed
        PostedByData malformedData = new PostedByData(null, "malformedUserName", "pic_malformed");
        List<PostedByData> followingList = Arrays.asList(followedUser1Data, malformedData);
        ResponseEntity<List<PostedByData>> responseEntity = createFollowingResponse(followingList);
        String expectedUri = apiEndpoint + "/api/follow/following/" + userId;

        when(restTemplate.exchange(eq(expectedUri), eq(HttpMethod.GET), isNull(), eq(LIST_POSTEDBYDATA_TYPE_REF)))
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
        assertEquals(post1.getId(), ((PostData) result.getContent().get(0).get("post")).getId());
        assertEquals(followedUser1Data.getName(),
                ((PostData) result.getContent().get(0).get("post")).getPostedBy().getName());

        verify(restTemplate).exchange(eq(expectedUri), eq(HttpMethod.GET), isNull(), eq(LIST_POSTEDBYDATA_TYPE_REF));
        // Verification for singleton list
        verify(postRepository).findByPostedByIn(eq(validFollowedIds));
    }

    @Test
    void processFeed_FetchFollowing_NetworkError() {
        // Arrange
        String expectedUri = apiEndpoint + "/api/follow/following/" + userId;
        when(restTemplate.exchange(eq(expectedUri), eq(HttpMethod.GET), isNull(), eq(LIST_POSTEDBYDATA_TYPE_REF)))
                .thenThrow(new ResourceAccessException("Network error"));

        // Act
        Page<Map<String, Object>> result = followingFeedStrategy.processFeed(null, queryDTO, null);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        assertEquals(0, result.getTotalElements());

        verify(restTemplate).exchange(eq(expectedUri), eq(HttpMethod.GET), isNull(), eq(LIST_POSTEDBYDATA_TYPE_REF));
        verify(postRepository, never()).findByPostedByIn(anyList());
    }

    @Test
    void processFeed_FetchFollowing_OtherException() {
        // Arrange
        String expectedUri = apiEndpoint + "/api/follow/following/" + userId;
        when(restTemplate.exchange(eq(expectedUri), eq(HttpMethod.GET), isNull(), eq(LIST_POSTEDBYDATA_TYPE_REF)))
                .thenThrow(new RuntimeException("Some other error"));

        // Act
        Page<Map<String, Object>> result = followingFeedStrategy.processFeed(null, queryDTO, null);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        assertEquals(0, result.getTotalElements());

        verify(restTemplate).exchange(eq(expectedUri), eq(HttpMethod.GET), isNull(), eq(LIST_POSTEDBYDATA_TYPE_REF));
        verify(postRepository, never()).findByPostedByIn(anyList());
    }

    @Test
    void processFeed_NoPostsFoundForFollowedUsers() {
        // Arrange
        List<PostedByData> followingList = Arrays.asList(followedUser1Data, followedUser2Data);
        ResponseEntity<List<PostedByData>> responseEntity = createFollowingResponse(followingList);
        String expectedUri = apiEndpoint + "/api/follow/following/" + userId;

        when(restTemplate.exchange(eq(expectedUri), eq(HttpMethod.GET), isNull(), eq(LIST_POSTEDBYDATA_TYPE_REF)))
                .thenReturn(responseEntity);

        List<UUID> expectedFollowedIds = new ArrayList<>(Arrays.asList(followedUserId1, followedUserId2));
        // REMOVED: Collections.sort(expectedFollowedIds);

        // Use argThat with HashSet comparison
        when(postRepository
                .findByPostedByIn(argThat(list -> new HashSet<>(list).equals(new HashSet<>(expectedFollowedIds)))))
                .thenReturn(Collections.emptyList()); // No posts found

        // Act
        Page<Map<String, Object>> result = followingFeedStrategy.processFeed(null, queryDTO, null);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        assertEquals(0, result.getTotalElements());

        verify(restTemplate).exchange(eq(expectedUri), eq(HttpMethod.GET), isNull(), eq(LIST_POSTEDBYDATA_TYPE_REF));
        // Use argThat for verification
        verify(postRepository)
                .findByPostedByIn(argThat(list -> new HashSet<>(list).equals(new HashSet<>(expectedFollowedIds))));
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

        List<PostedByData> followingList = Arrays.asList(followedUser1Data, followedUser2Data);
        ResponseEntity<List<PostedByData>> responseEntity = createFollowingResponse(followingList);
        String expectedUri = apiEndpoint + "/api/follow/following/" + userId;

        when(restTemplate.exchange(eq(expectedUri), eq(HttpMethod.GET), isNull(), eq(LIST_POSTEDBYDATA_TYPE_REF)))
                .thenReturn(responseEntity);

        List<UUID> followedIds = new ArrayList<>(Arrays.asList(followedUserId1, followedUserId2));
        List<Post> posts = Arrays.asList(post1, post2); // These posts exist but won't match filters

        // Use argThat with HashSet comparison
        when(postRepository.findByPostedByIn(argThat(list -> new HashSet<>(list).equals(new HashSet<>(followedIds)))))
                .thenReturn(posts); // Use sorted list

        // Act
        Page<Map<String, Object>> result = followingFeedStrategy.processFeed(null, queryDTO, null);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        assertEquals(0, result.getTotalElements()); // 0 because filtering removed all posts

        verify(restTemplate).exchange(eq(expectedUri), eq(HttpMethod.GET), isNull(), eq(LIST_POSTEDBYDATA_TYPE_REF));
        // Use argThat for verification
        verify(postRepository)
                .findByPostedByIn(argThat(list -> new HashSet<>(list).equals(new HashSet<>(followedIds))));
    }
}
