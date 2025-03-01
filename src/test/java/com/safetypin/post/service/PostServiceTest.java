package com.safetypin.post.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.safetypin.post.model.Post;
import com.safetypin.post.model.UserLocation;
import com.safetypin.post.repository.PostRepository;

@ExtendWith(MockitoExtension.class)
public class PostServiceTest {

    @Mock
    private PostRepository postRepository;
    
    @Mock
    private UserLocationService userLocationService;
    
    @InjectMocks
    private PostServiceImpl postService;
    
    private Post post1, post2, post3, post4;

    @BeforeEach
    void setUp() {
        // Create mock posts with different locations, categories, and dates
        // Using approximate distances: 1 degree ≈ 111km at the equator
        post1 = new Post(1L, "Post 1", "Content 1", "News", 
                LocalDateTime.of(2023, 1, 15, 10, 0), 0.05, 0.05);  // ~7.85 km from (0,0)
        
        post2 = new Post(2L, "Post 2", "Content 2", "Events", 
                LocalDateTime.of(2023, 2, 20, 14, 30), 0.7, 0.7);  // ~110 km from (0,0)
        
        post3 = new Post(3L, "Post 3", "Content 3", "News", 
                LocalDateTime.of(2022, 12, 10, 9, 15), 0.3, 0.3);  // ~47 km from (0,0)
                
        post4 = new Post(4L, "Post 4", "Content 4", "Announcements", 
                LocalDateTime.of(2023, 3, 5, 16, 45), 0.1, 0.1);  // ~16 km from (0,0)
    }

    @Test
    void fetchPostsWithinRadius() {
        // Setup: Mock repository to return all posts
        when(postRepository.findAll()).thenReturn(Arrays.asList(post1, post2, post3, post4));
        
        // Execute: Fetch posts within 50 km radius from (0,0)
        List<Post> result = postService.getPostsWithinRadius(0.0, 0.0, 50.0);
        
        // Verify: Only posts within 50 km are returned (post1, post3, post4)
        assertEquals(3, result.size());
        assertTrue(result.contains(post1));
        assertTrue(result.contains(post3));
        assertTrue(result.contains(post4));
        assertFalse(result.contains(post2));
    }

    @Test
    void applyCategoryFilter() {
        // Setup: Mock repository to return filtered posts
        when(postRepository.findByCategory("News")).thenReturn(Arrays.asList(post1, post3));
        
        // Execute: Fetch posts with category "News"
        List<Post> result = postService.getPostsByCategory("News");
        
        // Verify: Only posts with category "News" are returned
        assertEquals(2, result.size());
        assertTrue(result.contains(post1));
        assertTrue(result.contains(post3));
        assertFalse(result.contains(post2));
        assertFalse(result.contains(post4));
    }

    @Test
    void applyDateRangeFilter() {
        // Setup: Define date range and mock repository
        LocalDateTime startDate = LocalDateTime.of(2023, 1, 1, 0, 0);
        LocalDateTime endDate = LocalDateTime.of(2023, 3, 1, 0, 0);
        
        when(postRepository.findByTimestampBetween(startDate, endDate))
                .thenReturn(Arrays.asList(post1, post2));
        
        // Execute: Fetch posts within date range
        List<Post> result = postService.getPostsByDateRange(startDate, endDate);
        
        // Verify: Only posts within date range are returned
        assertEquals(2, result.size());
        assertTrue(result.contains(post1));
        assertTrue(result.contains(post2));
        assertFalse(result.contains(post3));
        assertFalse(result.contains(post4));
    }

    @Test
    void applyMultipleFilters() {
        // Setup: Define parameters for multiple filters
        LocalDateTime startDate = LocalDateTime.of(2023, 1, 1, 0, 0);
        LocalDateTime endDate = LocalDateTime.of(2023, 12, 31, 23, 59);
        String category = "News";
        double latitude = 0.0;
        double longitude = 0.0;
        double radius = 50.0;
        
        // Mock combined filters query
        when(postRepository.findByTimestampBetweenAndCategory(startDate, endDate, category))
                .thenReturn(Arrays.asList(post1));
        
        // Execute: Fetch posts with multiple filters
        List<Post> result = postService.getPostsWithFilters(latitude, longitude, radius, 
                                                           category, startDate, endDate);
        
        // Verify: Only posts matching all criteria are returned
        assertEquals(1, result.size());
        assertTrue(result.contains(post1));
        assertFalse(result.contains(post2));
        assertFalse(result.contains(post3));
        assertFalse(result.contains(post4));
    }

    @Test
    void dynamicLocationUpdate() {
        // Setup: Mock repository to return all posts
        when(postRepository.findAll()).thenReturn(Arrays.asList(post1, post2, post3, post4));
        
        // Execute: Fetch posts from initial location (0,0)
        List<Post> initialResults = postService.getPostsByProximity(0.0, 0.0);
        
        // Verify initial ordering (by distance from 0,0)
        assertEquals(4, initialResults.size());
        // Verify posts are ordered by proximity to (0,0)
        assertEquals(post1.getId(), initialResults.get(0).getId()); // Closest to (0,0)
        assertEquals(post4.getId(), initialResults.get(1).getId()); // Second closest
        
        // Execute: Fetch posts from updated location (1,1)
        List<Post> updatedResults = postService.getPostsByProximity(1.0, 1.0);
        
        // Verify updated ordering (by distance from 1,1)
        assertEquals(4, updatedResults.size());
        // Verify posts are now ordered by proximity to (1,1)
        assertEquals(post2.getId(), updatedResults.get(0).getId()); // Now closest to (1,1)
        assertNotEquals(initialResults.get(0).getId(), updatedResults.get(0).getId()); // Order has changed
    }

    @Test
    public void testSortPostsByProximity() {
        // Setup user location at origin
        UserLocation userLocation = new UserLocation(0.0, 0.0);
        
        // Create mock posts with different locations
        Post nearestPost = new Post();
        nearestPost.setId(1L);
        nearestPost.setLatitude(0.0);
        nearestPost.setLongitude(0.0); // At the origin (0,0) - distance = 0
        
        Post mediumPost = new Post();
        mediumPost.setId(2L);
        mediumPost.setLatitude(1.0);
        mediumPost.setLongitude(1.0); // At (1,1) - distance ≈ 1.41 units
        
        Post farthestPost = new Post();
        farthestPost.setId(3L);
        farthestPost.setLatitude(2.0);
        farthestPost.setLongitude(2.0); // At (2,2) - distance ≈ 2.83 units
        
        List<Post> unsortedPosts = Arrays.asList(farthestPost, mediumPost, nearestPost);
        
        // Mock repository to return unsorted posts
        when(postRepository.findAll()).thenReturn(unsortedPosts);
        when(userLocationService.getCurrentUserLocation()).thenReturn(userLocation);
        
        // Call service method
        List<Post> sortedPosts = postService.getPostsSortedByProximity();
        
        // Assert posts are ordered by increasing distance
        assertEquals(3, sortedPosts.size());
        assertEquals(nearestPost.getId(), sortedPosts.get(0).getId());
        assertEquals(mediumPost.getId(), sortedPosts.get(1).getId());
        assertEquals(farthestPost.getId(), sortedPosts.get(2).getId());
    }

    @Test
    public void testSortPostsWithEqualDistances() {
        // Setup user location
        UserLocation userLocation = new UserLocation(0.0, 0.0);
        
        // Create posts with equal distances but different timestamps
        Post olderPost = new Post();
        olderPost.setId(1L);
        olderPost.setLatitude(1.0);
        olderPost.setLongitude(1.0); // Distance = ~1.41
        olderPost.setCreatedAt(LocalDateTime.now().minusDays(2));
        
        Post newerPost = new Post();
        newerPost.setId(2L);
        newerPost.setLatitude(1.0);
        newerPost.setLongitude(1.0); // Same distance = ~1.41
        newerPost.setCreatedAt(LocalDateTime.now().minusDays(1));
        
        List<Post> unsortedPosts = Arrays.asList(olderPost, newerPost);
        
        // Mock repository and user location service
        when(postRepository.findAll()).thenReturn(unsortedPosts);
        when(userLocationService.getCurrentUserLocation()).thenReturn(userLocation);
        
        // Call service method
        List<Post> sortedPosts = postService.getPostsSortedByProximity();
        
        // Assert posts with equal distances are sorted by timestamp (newer first)
        assertEquals(2, sortedPosts.size());
        assertEquals(newerPost.getId(), sortedPosts.get(0).getId());
        assertEquals(olderPost.getId(), sortedPosts.get(1).getId());
    }

    @Test
    public void testRealTimeFilterUpdates() {
        // Setup test categories
        String sportsCategory = "Sports";
        String techCategory = "Technology";
        
        // Create posts with different categories
        Post sportsPost1 = new Post();
        sportsPost1.setId(1L);
        sportsPost1.setCategory(sportsCategory);
        
        Post sportsPost2 = new Post();
        sportsPost2.setId(2L);
        sportsPost2.setCategory(sportsCategory);
        
        Post techPost = new Post();
        techPost.setId(3L);
        techPost.setCategory(techCategory);
        
        List<Post> allPosts = Arrays.asList(sportsPost1, techPost, sportsPost2);
        
        // Mock repository for finding by category - removed unnecessary findAll() mock
        when(postRepository.findByCategory(sportsCategory))
            .thenReturn(Arrays.asList(sportsPost1, sportsPost2));
        
        // Call service method with filter
        List<Post> filteredPosts = postService.getPostsByCategory(sportsCategory);
        
        // Assert only sports posts are returned
        assertEquals(2, filteredPosts.size());
        for (Post post : filteredPosts) {
            assertEquals(sportsCategory, post.getCategory());
        }
        
        // Verify no delay in filter application by checking response immediacy
        // This is usually done with performance testing, but here we verify the method was called
        verify(postRepository).findByCategory(sportsCategory);
    }
}
