package com.safetypin.post.service.strategy;

import com.safetypin.post.dto.FeedQueryDTO;
import com.safetypin.post.model.Post;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class AbstractFeedStrategyTest {

    private TestFeedStrategy strategy;
    private Post safetyPost;
    private Post crimePost;
    private Post postWithNullCategory;
    private Post postWithNullTitle;
    private Post postWithNullCaption;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        strategy = new TestFeedStrategy();
        now = LocalDateTime.now();

        safetyPost = Post.builder()
                .id(UUID.randomUUID())
                .title("Safety Tips")
                .caption("Important safety information")
                .category("SAFETY")
                .createdAt(now)
                .latitude(0.0)
                .longitude(0.0)
                .build();

        crimePost = Post.builder()
                .id(UUID.randomUUID())
                .title("Crime Alert")
                .caption("Recent theft in neighborhood")
                .category("CRIME")
                .createdAt(now)
                .latitude(0.0)
                .longitude(0.0)
                .build();

        postWithNullCategory = Post.builder()
                .id(UUID.randomUUID())
                .title("Null Category")
                .caption("This post has a null category")
                .category(null)
                .createdAt(now)
                .latitude(0.0)
                .longitude(0.0)
                .build();

        postWithNullTitle = Post.builder()
                .id(UUID.randomUUID())
                .title(null)
                .caption("This post has a null title")
                .category("OTHER")
                .createdAt(now)
                .latitude(0.0)
                .longitude(0.0)
                .build();

        postWithNullCaption = Post.builder()
                .id(UUID.randomUUID())
                .title("Null Caption")
                .caption(null)
                .category("OTHER")
                .createdAt(now)
                .latitude(0.0)
                .longitude(0.0)
                .build();
    }

    @Test
    void matchesCategories_nullCategories_returnsTrue() {
        assertTrue(strategy.matchesCategories(safetyPost, null));
    }

    @Test
    void matchesCategories_emptyCategories_returnsTrue() {
        assertTrue(strategy.matchesCategories(safetyPost, Collections.emptyList()));
    }

    @Test
    void matchesCategories_matchingCategory_returnsTrue() {
        assertTrue(strategy.matchesCategories(crimePost, Arrays.asList("SAFETY", "CRIME")));
    }

    @Test
    void matchesCategories_nonMatchingCategory_returnsFalse() {
        assertFalse(strategy.matchesCategories(safetyPost, Arrays.asList("CRIME", "TRAFFIC")));
    }

    @Test
    void matchesCategories_nullPostCategory_returnsFalse() {
        assertFalse(strategy.matchesCategories(postWithNullCategory, List.of("SAFETY")));
    }

    @Test
    void matchesKeyword_nullKeyword_returnsTrue() {
        assertTrue(strategy.matchesKeyword(safetyPost, null));
    }

    @Test
    void matchesKeyword_emptyKeyword_returnsTrue() {
        assertTrue(strategy.matchesKeyword(safetyPost, ""));
    }

    @Test
    void matchesKeyword_matchesTitle_returnsTrue() {
        assertTrue(strategy.matchesKeyword(safetyPost, "safety"));
    }

    @Test
    void matchesKeyword_matchesCaption_returnsTrue() {
        assertTrue(strategy.matchesKeyword(safetyPost, "important"));
    }

    @Test
    void matchesKeyword_noMatch_returnsFalse() {
        assertFalse(strategy.matchesKeyword(safetyPost, "crime"));
    }

    @Test
    void matchesKeyword_nullTitle_searchesOnlyCaption() {
        assertTrue(strategy.matchesKeyword(postWithNullTitle, "null title"));
        assertFalse(strategy.matchesKeyword(postWithNullTitle, "not found"));
    }

    @Test
    void matchesKeyword_nullCaption_searchesOnlyTitle() {
        assertTrue(strategy.matchesKeyword(postWithNullCaption, "null caption"));
        assertFalse(strategy.matchesKeyword(postWithNullCaption, "not found"));
    }

    @Test
    void matchesDateRange_nullDateFrom_checksOnlyDateTo() {
        LocalDateTime yesterday = now.minusDays(1);
        Post oldPost = Post.builder()
                .createdAt(yesterday)
                .latitude(0.0)
                .longitude(0.0)
                .build();

        assertTrue(strategy.matchesDateRange(safetyPost, null, now.plusDays(1)));
        assertTrue(strategy.matchesDateRange(oldPost, null, now.plusDays(1)));
    }

    @Test
    void matchesDateRange_nullDateTo_checksOnlyDateFrom() {
        LocalDateTime tomorrow = now.plusDays(1);
        Post futurePost = Post.builder()
                .createdAt(tomorrow)
                .latitude(0.0)
                .longitude(0.0)
                .build();

        assertTrue(strategy.matchesDateRange(safetyPost, now.minusDays(1), null));
        assertTrue(strategy.matchesDateRange(futurePost, now.minusDays(1), null));
    }

    @Test
    void matchesDateRange_bothDatesProvided_checksBothConditions() {
        LocalDateTime yesterday = now.minusDays(1);
        LocalDateTime tomorrow = now.plusDays(1);

        // Post within range
        assertTrue(strategy.matchesDateRange(safetyPost, yesterday, tomorrow));

        // Post before range
        Post oldPost = Post.builder()
                .createdAt(yesterday.minusDays(1))
                .latitude(0.0)
                .longitude(0.0)
                .build();
        assertFalse(strategy.matchesDateRange(oldPost, yesterday, tomorrow));

        // Post after range
        Post futurePost = Post.builder()
                .createdAt(tomorrow.plusDays(1))
                .latitude(0.0)
                .longitude(0.0)
                .build();
        assertFalse(strategy.matchesDateRange(futurePost, yesterday, tomorrow));

        // Post at exact boundaries
        Post exactStartPost = Post.builder()
                .createdAt(yesterday)
                .latitude(0.0)
                .longitude(0.0)
                .build();
        Post exactEndPost = Post.builder()
                .createdAt(tomorrow)
                .latitude(0.0)
                .longitude(0.0)
                .build();
        assertTrue(strategy.matchesDateRange(exactStartPost, yesterday, tomorrow));
        assertTrue(strategy.matchesDateRange(exactEndPost, yesterday, tomorrow));
    }

    @Test
    void paginateResults_emptyList_returnsEmptyPage() {
        List<Map<String, Object>> results = Collections.emptyList();
        Pageable pageable = PageRequest.of(0, 10);

        Page<Map<String, Object>> page = strategy.paginateResults(results, pageable);

        assertTrue(page.isEmpty());
        assertEquals(0, page.getTotalElements());
    }

    @Test
    void paginateResults_startIndexBeyondSize_returnsEmptyPage() {
        List<Map<String, Object>> results = createTestResultList(5);
        Pageable pageable = PageRequest.of(2, 3); // Start at index 6, but we only have 5 items

        Page<Map<String, Object>> page = strategy.paginateResults(results, pageable);

        assertTrue(page.isEmpty());
        assertEquals(5, page.getTotalElements());
    }

    @Test
    void paginateResults_fullPage_returnsCorrectSublist() {
        List<Map<String, Object>> results = createTestResultList(5);
        Pageable pageable = PageRequest.of(0, 3);

        Page<Map<String, Object>> page = strategy.paginateResults(results, pageable);

        assertEquals(3, page.getContent().size());
        assertEquals(5, page.getTotalElements());
        assertEquals(0, page.getNumber());
        assertEquals(3, page.getSize());
    }

    @Test
    void paginateResults_partialLastPage_returnsCorrectSublist() {
        List<Map<String, Object>> results = createTestResultList(5);
        Pageable pageable = PageRequest.of(1, 3);

        Page<Map<String, Object>> page = strategy.paginateResults(results, pageable);

        assertEquals(2, page.getContent().size());
        assertEquals(5, page.getTotalElements());
        assertEquals(1, page.getNumber());
        assertEquals(3, page.getSize());
    }

    @Test
    void paginateResults_exactlyOneFullPage_returnsAllItems() {
        List<Map<String, Object>> results = createTestResultList(5);
        Pageable pageable = PageRequest.of(0, 5);

        Page<Map<String, Object>> page = strategy.paginateResults(results, pageable);

        assertEquals(5, page.getContent().size());
        assertEquals(5, page.getTotalElements());
        assertEquals(0, page.getNumber());
        assertEquals(5, page.getSize());
    }

    // Helper method to create a test list of results
    private List<Map<String, Object>> createTestResultList(int size) {
        List<Map<String, Object>> results = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", i);
            results.add(item);
        }
        return results;
    }

    // Concrete implementation for testing
    private static class TestFeedStrategy extends AbstractFeedStrategy {
        @Override
        public Page<Map<String, Object>> processFeed(List<Post> posts, FeedQueryDTO queryDTO) {
            // Simple implementation for testing
            List<Map<String, Object>> results = new ArrayList<>();
            return paginateResults(results, queryDTO.getPageable());
        }
    }
}
