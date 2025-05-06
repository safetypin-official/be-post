package com.safetypin.post.service.strategy;

import com.safetypin.post.dto.FeedQueryDTO;
import com.safetypin.post.dto.PostData;
import com.safetypin.post.dto.PostedByData;
import com.safetypin.post.model.Post;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class DistanceFeedStrategyTest {

    // User location (for distance calculations)
    private final Double userLat = 0.0;
    private final Double userLon = 0.0;
    private DistanceFeedStrategy strategy;
    private List<Post> posts;
    private UUID testUserId;
    private LocalDateTime now;
    private LocalDateTime yesterday;
    private LocalDateTime lastWeek;

    @BeforeEach
    void setUp() {
        strategy = new DistanceFeedStrategy();
        testUserId = UUID.randomUUID();
        now = LocalDateTime.now();
        yesterday = now.minusDays(1);
        lastWeek = now.minusWeeks(1);

        // Create test posts with different locations and properties
        Post post1 = Post.builder()
                .id(UUID.randomUUID())
                .title("Nearby Post")
                .caption("This is very close to the user")
                .category("SAFETY")
                .createdAt(yesterday) // Not the newest
                .latitude(0.01) // Very close to user
                .longitude(0.01)
                .postedBy(UUID.randomUUID())
                .build();

        Post post2 = Post.builder()
                .id(UUID.randomUUID())
                .title("Medium Distance Post")
                .caption("This is at medium distance")
                .category("CRIME")
                .createdAt(now) // Newest post
                .latitude(0.1) // Medium distance from user
                .longitude(0.1)
                .postedBy(UUID.randomUUID())
                .build();

        Post post3 = Post.builder()
                .id(UUID.randomUUID())
                .title("Far Away Post")
                .caption("This is far from the user")
                .category("TRAFFIC")
                .createdAt(lastWeek) // Oldest post
                .latitude(1.0) // Far from user
                .longitude(1.0)
                .postedBy(UUID.randomUUID())
                .build();

        posts = Arrays.asList(post1, post2, post3);
    }

    @Test
    void processFeed_nullCoordinates_throwsIllegalArgumentException() {
        // Arrange
        FeedQueryDTO queryDTO = FeedQueryDTO.builder()
                .userId(testUserId)
                .pageable(PageRequest.of(0, 10))
                .build();

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> strategy.processFeed(posts, queryDTO, null));
    }

    @Test
    void processFeed_nullLatitude_throwsIllegalArgumentException() {
        // Arrange
        FeedQueryDTO queryDTO = FeedQueryDTO.builder()
                .userId(testUserId)
                .userLon(userLon)
                .pageable(PageRequest.of(0, 10))
                .build();

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> strategy.processFeed(posts, queryDTO, null));
    }

    @Test
    void processFeed_nullLongitude_throwsIllegalArgumentException() {
        // Arrange
        FeedQueryDTO queryDTO = FeedQueryDTO.builder()
                .userId(testUserId)
                .userLat(userLat)
                .pageable(PageRequest.of(0, 10))
                .build();

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> strategy.processFeed(posts, queryDTO, null));
    }

    @Test
    void processFeed_emptyList_returnsEmptyPage() {
        // Arrange
        FeedQueryDTO queryDTO = FeedQueryDTO.builder()
                .userId(testUserId)
                .userLat(userLat)
                .userLon(userLon)
                .pageable(PageRequest.of(0, 10))
                .build();

        // Act
        Page<Map<String, Object>> result = strategy.processFeed(Collections.emptyList(), queryDTO, null);

        // Assert
        assertTrue(result.isEmpty());
        assertEquals(0, result.getTotalElements());
    }

    @Test
    void processFeed_noFilters_returnsAllPostsSortedByDistance() {
        // Arrange
        FeedQueryDTO queryDTO = FeedQueryDTO.builder()
                .userId(testUserId)
                .userLat(userLat)
                .userLon(userLon)
                .pageable(PageRequest.of(0, 10))
                .build();

        // Act
        Page<Map<String, Object>> result = strategy.processFeed(posts, queryDTO, null);

        // Assert
        assertEquals(3, result.getTotalElements());
        List<Map<String, Object>> content = result.getContent();

        // Check sorting (nearest first)
        assertEquals("Nearby Post", ((PostData) content.get(0).get("post")).getTitle());
        assertEquals("Medium Distance Post", ((PostData) content.get(1).get("post")).getTitle());
        assertEquals("Far Away Post", ((PostData) content.get(2).get("post")).getTitle());

        // Check that distances are included and properly sorted
        Double distance1 = (Double) content.get(0).get("distance");
        Double distance2 = (Double) content.get(1).get("distance");
        Double distance3 = (Double) content.get(2).get("distance");

        assertTrue(distance1 < distance2);
        assertTrue(distance2 < distance3);
    }

    @Test
    void processFeed_withCategoryFilter_returnsFilteredPosts() {
        // Arrange
        FeedQueryDTO queryDTO = FeedQueryDTO.builder()
                .userId(testUserId)
                .userLat(userLat)
                .userLon(userLon)
                .categories(List.of("CRIME"))
                .pageable(PageRequest.of(0, 10))
                .build();

        // Act
        Page<Map<String, Object>> result = strategy.processFeed(posts, queryDTO, null);

        // Assert
        assertEquals(1, result.getTotalElements());
        assertEquals("Medium Distance Post", ((PostData) result.getContent().getFirst().get("post")).getTitle());
    }

    @Test
    void processFeed_withKeywordFilter_returnsFilteredPosts() {
        // Arrange
        FeedQueryDTO queryDTO = FeedQueryDTO.builder()
                .userId(testUserId)
                .userLat(userLat)
                .userLon(userLon)
                .keyword("far")
                .pageable(PageRequest.of(0, 10))
                .build();

        // Act
        Page<Map<String, Object>> result = strategy.processFeed(posts, queryDTO, null);

        // Assert
        assertEquals(1, result.getTotalElements());
        assertEquals("Far Away Post", ((PostData) result.getContent().getFirst().get("post")).getTitle());
    }

    @Test
    void processFeed_withDateFromFilter_returnsFilteredPosts() {
        // Arrange
        FeedQueryDTO queryDTO = FeedQueryDTO.builder()
                .userId(testUserId)
                .userLat(userLat)
                .userLon(userLon)
                .dateFrom(yesterday.minusHours(1))
                .pageable(PageRequest.of(0, 10))
                .build();

        // Act
        Page<Map<String, Object>> result = strategy.processFeed(posts, queryDTO, null);

        // Assert
        assertEquals(2, result.getTotalElements());
    }

    @Test
    void processFeed_withDateToFilter_returnsFilteredPosts() {
        // Arrange
        FeedQueryDTO queryDTO = FeedQueryDTO.builder()
                .userId(testUserId)
                .userLat(userLat)
                .userLon(userLon)
                .dateTo(yesterday.plusHours(1))
                .pageable(PageRequest.of(0, 10))
                .build();

        // Act
        Page<Map<String, Object>> result = strategy.processFeed(posts, queryDTO, null);

        // Assert
        assertEquals(2, result.getTotalElements());
    }

    @Test
    void processFeed_withMultipleFilters_returnsFilteredPosts() {
        // Arrange
        FeedQueryDTO queryDTO = FeedQueryDTO.builder()
                .userId(testUserId)
                .userLat(userLat)
                .userLon(userLon)
                .categories(List.of("SAFETY"))
                .dateFrom(lastWeek)
                .dateTo(yesterday.plusHours(1))
                .pageable(PageRequest.of(0, 10))
                .build();

        // Act
        Page<Map<String, Object>> result = strategy.processFeed(posts, queryDTO, null);

        // Assert
        assertEquals(1, result.getTotalElements());
        assertEquals("Nearby Post", ((PostData) result.getContent().getFirst().get("post")).getTitle());
    }

    @Test
    void processFeed_withPagination_returnsPaginatedResults() {
        // Arrange
        FeedQueryDTO queryDTO = FeedQueryDTO.builder()
                .userId(testUserId)
                .userLat(userLat)
                .userLon(userLon)
                .pageable(PageRequest.of(0, 2))
                .build();

        // Act
        Page<Map<String, Object>> result = strategy.processFeed(posts, queryDTO, null);

        // Assert
        assertEquals(2, result.getContent().size());
        assertEquals(3, result.getTotalElements());
        assertEquals(0, result.getNumber());
        assertEquals(2, result.getSize());
        assertEquals(2, result.getNumberOfElements());
    }

    @Test
    void processFeed_withSecondPage_returnsSecondPageResults() {
        // Arrange
        FeedQueryDTO queryDTO = FeedQueryDTO.builder()
                .userId(testUserId)
                .userLat(userLat)
                .userLon(userLon)
                .pageable(PageRequest.of(1, 2))
                .build();

        // Act
        Page<Map<String, Object>> result = strategy.processFeed(posts, queryDTO, null);

        // Assert
        assertEquals(1, result.getContent().size());
        assertEquals(3, result.getTotalElements());
        assertEquals(1, result.getNumber());
        assertEquals(2, result.getSize());
        assertEquals(1, result.getNumberOfElements());
    }

    @Test
    void processFeed_withPostsHavingNullCreatedAt_handlesGracefully() {
        // Create a post with null createdAt
        Post nullCreatedAtPost = Post.builder()
                .id(UUID.randomUUID())
                .title("Null CreatedAt Post")
                .caption("This post has null createdAt")
                .category("OTHER")
                .createdAt(null) // Null createdAt
                .latitude(0.5)
                .longitude(0.5)
                .postedBy(UUID.randomUUID())
                .build();

        List<Post> testPosts = new ArrayList<>(posts);
        testPosts.add(nullCreatedAtPost);

        // Arrange
        FeedQueryDTO queryDTO = FeedQueryDTO.builder()
                .userId(testUserId)
                .userLat(userLat)
                .userLon(userLon)
                .dateFrom(yesterday.minusHours(1)) // Should filter out the null createdAt post
                .pageable(PageRequest.of(0, 10))
                .build();

        // Act
        Page<Map<String, Object>> result = strategy.processFeed(testPosts, queryDTO, null);

        // Assert
        assertEquals(3, result.getTotalElements());
    }

    @Test
    void processFeed_withSmallRadius_returnsOnlyNearbyPosts() {
        // Arrange - use a small radius that only includes the nearest post
        double smallRadius = 5.0; // Small enough to only include post1
        
        FeedQueryDTO queryDTO = FeedQueryDTO.builder()
                .userId(testUserId)
                .userLat(userLat)
                .userLon(userLon)
                .radius(smallRadius)
                .pageable(PageRequest.of(0, 10))
                .build();

        // Act
        Page<Map<String, Object>> result = strategy.processFeed(posts, queryDTO, null);

        // Assert
        assertEquals(1, result.getTotalElements());
        assertEquals("Nearby Post", ((PostData) result.getContent().getFirst().get("post")).getTitle());
    }

    @Test
    void processFeed_withMediumRadius_returnsNearbyAndMediumPosts() {
        // Arrange - use a medium radius that includes the first two posts
        double mediumRadius = 20.0; // Enough to include post1 and post2, but not post3
        
        FeedQueryDTO queryDTO = FeedQueryDTO.builder()
                .userId(testUserId)
                .userLat(userLat)
                .userLon(userLon)
                .radius(mediumRadius)
                .pageable(PageRequest.of(0, 10))
                .build();

        // Act
        Page<Map<String, Object>> result = strategy.processFeed(posts, queryDTO, null);

        // Assert
        assertEquals(2, result.getTotalElements());
        List<Map<String, Object>> content = result.getContent();
        
        // Check that we have both the nearby and medium posts (sorted by distance)
        assertEquals("Nearby Post", ((PostData) content.get(0).get("post")).getTitle());
        assertEquals("Medium Distance Post", ((PostData) content.get(1).get("post")).getTitle());
    }

    @Test
    void processFeed_withLargeRadius_returnsAllPosts() {
        // Arrange - use a large radius that includes all posts
        double largeRadius = 200.0; // Enough to include all posts
        
        FeedQueryDTO queryDTO = FeedQueryDTO.builder()
                .userId(testUserId)
                .userLat(userLat)
                .userLon(userLon)
                .radius(largeRadius)
                .pageable(PageRequest.of(0, 10))
                .build();

        // Act
        Page<Map<String, Object>> result = strategy.processFeed(posts, queryDTO, null);

        // Assert
        assertEquals(3, result.getTotalElements());
    }

    @Test
    void processFeed_withNullRadius_usesDefaultRadius() {
        // Arrange - don't specify a radius, should use default of 10.0
        // At default radius of 10.0, only post1 should be included
        
        FeedQueryDTO queryDTO = FeedQueryDTO.builder()
                .userId(testUserId)
                .userLat(userLat)
                .userLon(userLon)
                .radius(null) // Null radius
                .pageable(PageRequest.of(0, 10))
                .build();

        // Act
        Page<Map<String, Object>> result = strategy.processFeed(posts, queryDTO, null);

        // Assert
        assertEquals(3, result.getTotalElements());
        assertEquals("Nearby Post", ((PostData) result.getContent().getFirst().get("post")).getTitle());
    }

    @Test
    void processFeed_withZeroRadius_returnsNoPosts() {
        // Arrange - use a radius of 0, which should include no posts
        double zeroRadius = 0.0;
        
        FeedQueryDTO queryDTO = FeedQueryDTO.builder()
                .userId(testUserId)
                .userLat(userLat)
                .userLon(userLon)
                .radius(zeroRadius)
                .pageable(PageRequest.of(0, 10))
                .build();

        // Act
        Page<Map<String, Object>> result = strategy.processFeed(posts, queryDTO, null);

        // Assert
        assertEquals(0, result.getTotalElements());
        assertTrue(result.isEmpty());
    }

    @Test
    void processFeed_withNegativeRadius_treatsAsZeroRadius() {
        // Arrange - use a negative radius (invalid value)
        double negativeRadius = -5.0;
        
        FeedQueryDTO queryDTO = FeedQueryDTO.builder()
                .userId(testUserId)
                .userLat(userLat)
                .userLon(userLon)
                .radius(negativeRadius)
                .pageable(PageRequest.of(0, 10))
                .build();

        // Act
        Page<Map<String, Object>> result = strategy.processFeed(posts, queryDTO, null);

        // Assert
        // Negative radius should be treated as radius 0, meaning no posts are returned
        assertEquals(0, result.getTotalElements());
        assertTrue(result.isEmpty(), "No posts should be returned with negative radius");
    }

    @Test
    void processFeed_withNullProfileList_handlesGracefully() {
        // Arrange
        FeedQueryDTO queryDTO = FeedQueryDTO.builder()
                .userId(testUserId)
                .userLat(userLat)
                .userLon(userLon)
                .pageable(PageRequest.of(0, 10))
                .build();
        
        // Act - explicitly pass null for profileList
        Page<Map<String, Object>> result = strategy.processFeed(posts, queryDTO, null);
        
        // Assert
        assertFalse(result.isEmpty());
        
        // Verify all returned posts have null postedBy data
        for (Map<String, Object> postMap : result.getContent()) {
            PostData postData = (PostData) postMap.get("post");
            assertNull(postData.getPostedBy(), "PostedBy data should be null when profileList is null");
        }
    }

    @Test
    void processFeed_withNonNullProfileList_includesProfileData() {
        // Arrange
        UUID poster1Id = posts.get(0).getPostedBy();
        UUID poster2Id = posts.get(1).getPostedBy();
        UUID poster3Id = posts.get(2).getPostedBy();
        
        Map<UUID, PostedByData> profileMap = new HashMap<>();
        profileMap.put(poster1Id, new PostedByData(poster1Id, "User One", "avatar1.jpg"));
        profileMap.put(poster2Id, new PostedByData(poster2Id, "User Two", "avatar2.jpg"));
        profileMap.put(poster3Id, new PostedByData(poster3Id, "User Three", "avatar3.jpg"));
        
        FeedQueryDTO queryDTO = FeedQueryDTO.builder()
                .userId(testUserId)
                .userLat(userLat)
                .userLon(userLon)
                .pageable(PageRequest.of(0, 10))
                .build();
        
        // Act - pass non-null profileList
        Page<Map<String, Object>> result = strategy.processFeed(posts, queryDTO, profileMap);
        
        // Assert
        assertFalse(result.isEmpty());
        
        // Verify returned posts have correct postedBy data
        for (Map<String, Object> postMap : result.getContent()) {
            PostData postData = (PostData) postMap.get("post");
            assertNotNull(postData, "PostData should not be null");
            assertNotNull(postData.getPostedBy(), "PostedBy data should not be null");
            
            UUID postedById = postData.getPostedBy().getUserId();
            assertTrue(profileMap.containsKey(postedById), "Profile data should match the post author");
            assertEquals(profileMap.get(postedById).getName(), 
                         postData.getPostedBy().getName(), 
                         "Username should match from profile map");
        }
    }
}
