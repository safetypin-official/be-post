package com.safetypin.post.service.strategy;

import com.safetypin.post.dto.FeedQueryDTO;
import com.safetypin.post.dto.PostData;
import com.safetypin.post.model.Post;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TimestampFeedStrategyTest {

    private TimestampFeedStrategy strategy;
    private List<Post> posts;
    private UUID testUserId;
    private LocalDateTime now;
    private LocalDateTime yesterday;
    private LocalDateTime lastWeek;

    @BeforeEach
    void setUp() {
        strategy = new TimestampFeedStrategy();
        testUserId = UUID.randomUUID();
        now = LocalDateTime.now();
        yesterday = now.minusDays(1);
        lastWeek = now.minusWeeks(1);

        // Create test posts with different properties
        Post post1 = Post.builder()
                .id(UUID.randomUUID())
                .title("Safety Tips")
                .caption("Important safety information")
                .category("SAFETY")
                .createdAt(now)
                .latitude(1.0)
                .longitude(1.0)
                .postedBy(UUID.randomUUID())
                .build();

        Post post2 = Post.builder()
                .id(UUID.randomUUID())
                .title("Crime Alert")
                .caption("Recent theft in neighborhood")
                .category("CRIME")
                .createdAt(yesterday)
                .latitude(2.0)
                .longitude(2.0)
                .postedBy(UUID.randomUUID())
                .build();

        Post post3 = Post.builder()
                .id(UUID.randomUUID())
                .title("Traffic Update")
                .caption("Road closure on Main Street")
                .category("TRAFFIC")
                .createdAt(lastWeek)
                .latitude(3.0)
                .longitude(3.0)
                .postedBy(UUID.randomUUID())
                .build();

        posts = Arrays.asList(post1, post2, post3);
    }

    @Test
    void processFeed_emptyList_returnsEmptyPage() {
        // Arrange
        FeedQueryDTO queryDTO = FeedQueryDTO.builder()
                .userId(testUserId)
                .pageable(PageRequest.of(0, 10))
                .build();

        // Act
        Page<Map<String, Object>> result = strategy.processFeed(Collections.emptyList(), queryDTO, null);

        // Assert
        assertTrue(result.isEmpty());
        assertEquals(0, result.getTotalElements());
    }

    @Test
    void processFeed_noFilters_returnsAllPostsSortedByTimestamp() {
        // Arrange
        FeedQueryDTO queryDTO = FeedQueryDTO.builder()
                .userId(testUserId)
                .pageable(PageRequest.of(0, 10))
                .build();

        // Act
        Page<Map<String, Object>> result = strategy.processFeed(posts, queryDTO, null);

        // Assert
        assertEquals(3, result.getTotalElements());
        List<Map<String, Object>> content = result.getContent();

        // Check sorting (newest first)
        assertEquals("Safety Tips", ((PostData) content.get(0).get("post")).getTitle());
        assertEquals("Crime Alert", ((PostData) content.get(1).get("post")).getTitle());
        assertEquals("Traffic Update", ((PostData) content.get(2).get("post")).getTitle());
    }

    @Test
    void processFeed_withCategoryFilter_returnsFilteredPosts() {
        // Arrange
        FeedQueryDTO queryDTO = FeedQueryDTO.builder()
                .userId(testUserId)
                .categories(List.of("SAFETY"))
                .pageable(PageRequest.of(0, 10))
                .build();

        // Act
        Page<Map<String, Object>> result = strategy.processFeed(posts, queryDTO, null);

        // Assert
        assertEquals(1, result.getTotalElements());
        assertEquals("Safety Tips", ((PostData) result.getContent().get(0).get("post")).getTitle());
    }

    @Test
    void processFeed_withKeywordFilter_returnsFilteredPosts() {
        // Arrange
        FeedQueryDTO queryDTO = FeedQueryDTO.builder()
                .userId(testUserId)
                .keyword("theft")
                .pageable(PageRequest.of(0, 10))
                .build();

        // Act
        Page<Map<String, Object>> result = strategy.processFeed(posts, queryDTO, null);

        // Assert
        assertEquals(1, result.getTotalElements());
        assertEquals("Crime Alert", ((PostData) result.getContent().get(0).get("post")).getTitle());
    }

    @Test
    void processFeed_withDateFromFilter_returnsFilteredPosts() {
        // Arrange
        FeedQueryDTO queryDTO = FeedQueryDTO.builder()
                .userId(testUserId)
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
                .categories(Arrays.asList("CRIME", "TRAFFIC"))
                .dateFrom(lastWeek)
                .dateTo(yesterday.plusHours(1))
                .pageable(PageRequest.of(0, 10))
                .build();

        // Act
        Page<Map<String, Object>> result = strategy.processFeed(posts, queryDTO, null);

        // Assert
        assertEquals(2, result.getTotalElements());
    }

    @Test
    void processFeed_withPagination_returnsPaginatedResults() {
        // Arrange
        FeedQueryDTO queryDTO = FeedQueryDTO.builder()
                .userId(testUserId)
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
    void processFeed_withEmptyPageResult_returnsEmptyContent() {
        // Arrange
        FeedQueryDTO queryDTO = FeedQueryDTO.builder()
                .userId(testUserId)
                .pageable(PageRequest.of(2, 2))
                .build();

        // Act
        Page<Map<String, Object>> result = strategy.processFeed(posts, queryDTO, null);

        // Assert
        assertTrue(result.isEmpty());
        assertEquals(3, result.getTotalElements());
        assertEquals(0, result.getNumberOfElements());
    }

    @Test
    void processFeed_withKeywordFilterOnNullFields_returnsNoMatches() {
        // Create a post with null title and caption
        Post nullFieldsPost = Post.builder()
                .id(UUID.randomUUID())
                .title(null)
                .caption(null)
                .category("OTHER")
                .createdAt(now)
                .latitude(4.0)
                .longitude(4.0)
                .postedBy(UUID.randomUUID())
                .build();

        List<Post> testPosts = Collections.singletonList(nullFieldsPost);

        // Arrange
        FeedQueryDTO queryDTO = FeedQueryDTO.builder()
                .userId(testUserId)
                .keyword("anyKeyword")
                .pageable(PageRequest.of(0, 10))
                .build();

        // Act
        Page<Map<String, Object>> result = strategy.processFeed(testPosts, queryDTO, null);

        // Assert
        assertEquals(0, result.getTotalElements());
        assertTrue(result.isEmpty());
    }

    @Test
    void processFeed_withMultipleCategoriesFilter_returnsFilteredPosts() {
        // Arrange
        FeedQueryDTO queryDTO = FeedQueryDTO.builder()
                .userId(testUserId)
                .categories(Arrays.asList("SAFETY", "CRIME"))
                .pageable(PageRequest.of(0, 10))
                .build();

        // Act
        Page<Map<String, Object>> result = strategy.processFeed(posts, queryDTO, null);

        // Assert
        assertEquals(2, result.getTotalElements());
    }
}
