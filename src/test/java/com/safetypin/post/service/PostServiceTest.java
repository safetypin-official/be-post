package com.safetypin.post.service;

import com.safetypin.post.model.Post;
import com.safetypin.post.repository.PostRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.*;

import com.safetypin.post.exception.InvalidPostDataException;
import com.safetypin.post.exception.PostException;
import com.safetypin.post.service.filter.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    private final LocalDateTime now = LocalDateTime.now();
    private final LocalDateTime yesterday = now.minusDays(1);
    private final LocalDateTime tomorrow = now.plusDays(1);
    @Mock
    private PostRepository postRepository;
    private GeometryFactory geometryFactory;
    private PostServiceImpl postService;
    private Post post1;
    private Post post2;
    private Post post3;
    private Post postWithoutLocation;

    @BeforeEach
    void setup() {
        geometryFactory = new GeometryFactory();
        postService = new PostServiceImpl(postRepository, geometryFactory);

        // Create test posts with locations
        post1 = new Post();
        post1.setLocation(geometryFactory.createPoint(new Coordinate(0.1, 0.1))); // lon, lat
        post1.setCategory("safety");
        post1.setCreatedAt(now);

        post2 = new Post();
        post2.setLocation(geometryFactory.createPoint(new Coordinate(0.2, 0.2))); // lon, lat
        post2.setCategory("crime");
        post2.setCreatedAt(yesterday);

        post3 = new Post();
        post3.setLocation(geometryFactory.createPoint(new Coordinate(0.3, 0.3))); // lon, lat
        post3.setCategory("safety");
        post3.setCreatedAt(tomorrow);

        postWithoutLocation = new Post();
        postWithoutLocation.setCategory("safety");
        postWithoutLocation.setCreatedAt(now);
    }

    @Test
    void testFindPostsByLocation_WithLocation() {
        // Given
        double centerLat = 0.1;
        double centerLon = 0.1;
        double radius = 10.0;
        Pageable pageable = PageRequest.of(0, 10);
        List<Post> postList = Arrays.asList(post1, post2);
        Page<Post> postPage = new PageImpl<>(postList, pageable, postList.size());

        when(postRepository.findPostsWithFilter(
                any(), anyDouble(), any(), any(), any(), any(PageRequest.class)
        )).thenReturn(postPage);

        // When
        Page<Map<String, Object>> result = postService.findPostsByLocation(
                centerLat, centerLon, radius, null, null, null, pageable);

        // Then
        assertEquals(2, result.getContent().size());

        Post resultPost1 = (Post) result.getContent().getFirst().get("post");
        assertEquals(post1, resultPost1);

        Double distance1 = (Double) result.getContent().getFirst().get("distance");
        assertEquals(0.0, distance1, 0.001); // Same location, distance should be 0
    }

    @Test
    void testFindPostsByLocation_WithoutLocation() {
        // Given
        double centerLat = 0.1;
        double centerLon = 0.1;
        double radius = 10.0;
        Pageable pageable = PageRequest.of(0, 10);
        List<Post> postList = Collections.singletonList(postWithoutLocation);
        Page<Post> postPage = new PageImpl<>(postList, pageable, postList.size());

        when(postRepository.findPostsWithFilter(
                any(), anyDouble(), any(), any(), any(), any(PageRequest.class)
        )).thenReturn(postPage);

        // When
        Page<Map<String, Object>> result = postService.findPostsByLocation(
                centerLat, centerLon, radius, null, null, null, pageable);
        // Then
        assertEquals(1, result.getContent().size());
        assertNull(result.getContent().getFirst().get("distance"));
    }

    @Test
    void testSearchPostsWithinRadius() {
        // Given
        double centerLat = 0.1;
        double centerLon = 0.1;
        double radius = 10.0;
        Point centerPoint = geometryFactory.createPoint(new Coordinate(centerLon, centerLat));
        Pageable pageable = PageRequest.of(0, 10);
        List<Post> postList = Arrays.asList(post1, post2);
        Page<Post> expected = new PageImpl<>(postList, pageable, postList.size());

        when(postRepository.findPostsWithinPointAndRadius(centerPoint, radius, pageable))
                .thenReturn(expected);

        // When
        Page<Post> result = postService.searchPostsWithinRadius(centerPoint, radius, pageable);

        // Then
        assertEquals(expected, result);
    }

    @Test
    void testGetPostsWithinRadius() {
        // Given
        double centerLat = 0.1;
        double centerLon = 0.1;
        double radius = 150.0; // km - increased radius to include all test posts

        List<Post> allPosts = Arrays.asList(post1, post2, post3, postWithoutLocation);

        when(postRepository.findAll()).thenReturn(allPosts);

        // When
        List<Post> result = postService.getPostsWithinRadius(centerLat, centerLon, radius);

        // Then
        // Post without location should be filtered out
        assertEquals(3, result.size());
        assertTrue(result.contains(post1));
        assertTrue(result.contains(post2));
        assertTrue(result.contains(post3));
        assertFalse(result.contains(postWithoutLocation));
    }

    @Test
    void testGetPostsByCategory() {
        // Given
        String category = "safety";
        List<Post> expectedPosts = Arrays.asList(post1, post3);

        when(postRepository.findByCategory(category)).thenReturn(expectedPosts);

        // When
        List<Post> result = postService.getPostsByCategory(category);

        // Then
        assertEquals(expectedPosts, result);
    }

    @Test
    void testGetPostsByDateRange() {
        // Given
        LocalDateTime startDate = yesterday;
        LocalDateTime endDate = tomorrow;
        List<Post> expectedPosts = Arrays.asList(post1, post2, post3);

        when(postRepository.findByCreatedAtBetween(startDate, endDate)).thenReturn(expectedPosts);

        // When
        List<Post> result = postService.getPostsByDateRange(startDate, endDate);

        // Then
        assertEquals(expectedPosts, result);
    }

    @Test
    void testGetPostsWithFilters_AllFilters() {
        // Given
        double centerLat = 0.1;
        double centerLon = 0.1;
        double radius = 150.0; // Increased radius to include all test posts
        String category = "safety";
        LocalDateTime startDate = yesterday;
        LocalDateTime endDate = tomorrow;

        List<Post> allPosts = Arrays.asList(post1, post2, post3, postWithoutLocation);

        // Mock findAll() instead of findByTimestampBetweenAndCategory()
        when(postRepository.findAll()).thenReturn(allPosts);

        // When
        List<Post> result = postService.getPostsWithFilters(
                centerLat, centerLon, radius, category, startDate, endDate);

        // Then
        assertEquals(2, result.size()); // postWithoutLocation should be filtered out
        assertTrue(result.contains(post1));
        assertTrue(result.contains(post3));
        assertFalse(result.contains(postWithoutLocation));
        assertFalse(result.contains(post2)); // Different category, should be filtered out
    }

    @Test
    void testGetPostsWithFilters_OnlyCategoryFilter() {
        // Given
        double centerLat = 0.1;
        double centerLon = 0.1;
        double radius = 150.0; // Increased radius
        String category = "safety";

        List<Post> allPosts = Arrays.asList(post1, post2, post3, postWithoutLocation);

        // Mock findAll() instead of findByCategory()
        when(postRepository.findAll()).thenReturn(allPosts);

        // When
        List<Post> result = postService.getPostsWithFilters(
                centerLat, centerLon, radius, category, null, null);

        // Then
        assertEquals(2, result.size()); // postWithoutLocation should be filtered out
        assertTrue(result.contains(post1));
        assertTrue(result.contains(post3));
        assertFalse(result.contains(postWithoutLocation));
        assertFalse(result.contains(post2)); // Different category, should be filtered out
    }

    @Test
    void testGetPostsWithFilters_OnlyDateFilter() {
        // Given
        double centerLat = 0.1;
        double centerLon = 0.1;
        double radius = 150.0; // Increased radius
        LocalDateTime startDate = yesterday;
        LocalDateTime endDate = tomorrow;

        List<Post> allPosts = Arrays.asList(post1, post2, post3, postWithoutLocation);

        // Mock findAll() instead of findByCreatedAtBetween() since we now use strategy pattern
        when(postRepository.findAll()).thenReturn(allPosts);

        // When
        List<Post> result = postService.getPostsWithFilters(
                centerLat, centerLon, radius, null, startDate, endDate);

        // Then
        assertEquals(3, result.size()); // postWithoutLocation should be filtered out
        assertTrue(result.contains(post1));
        assertTrue(result.contains(post2));
        assertTrue(result.contains(post3));
        assertFalse(result.contains(postWithoutLocation));
    }

    @Test
    void testGetPostsWithFilters_NoFilters() {
        // Given
        double centerLat = 0.1;
        double centerLon = 0.1;
        double radius = 150.0; // Increased radius

        List<Post> allPosts = Arrays.asList(post1, post2, post3, postWithoutLocation);

        when(postRepository.findAll()).thenReturn(allPosts);

        // When
        List<Post> result = postService.getPostsWithFilters(
                centerLat, centerLon, radius, null, null, null);

        // Then
        assertEquals(3, result.size()); // postWithoutLocation should be filtered out
        assertTrue(result.contains(post1));
        assertTrue(result.contains(post2));
        assertTrue(result.contains(post3));
        assertFalse(result.contains(postWithoutLocation));
    }

    @Test
    void testGetPostsByProximity() {
        // Given
        double centerLat = 0.1;
        double centerLon = 0.1;

        List<Post> allPosts = Arrays.asList(post3, post1, post2, postWithoutLocation);

        when(postRepository.findAll()).thenReturn(allPosts);

        // When
        List<Post> result = postService.getPostsByProximity(centerLat, centerLon);

        // Then
        assertEquals(3, result.size()); // postWithoutLocation should be filtered out
        // Posts should be sorted by distance from the center point
        assertEquals(post1, result.get(0)); // closest to center
        assertEquals(post2, result.get(1));
        assertEquals(post3, result.get(2)); // furthest from center
    }

    @Test
    void testCalculateDistance() {
        // Given
        double lat1 = 0.0;
        double lon1 = 0.0;
        double lat2 = 1.0;
        double lon2 = 1.0;
        double radius = 200.0; // km - large enough to include all test posts

        // When
        // We can indirectly test the private calculateDistance method
        // through the public getPostsWithinRadius method
        List<Post> posts = new ArrayList<>();

        Post postAtOrigin = new Post();
        postAtOrigin.setLocation(geometryFactory.createPoint(new Coordinate(lon1, lat1)));
        posts.add(postAtOrigin);

        Post postAtDistance = new Post();
        postAtDistance.setLocation(geometryFactory.createPoint(new Coordinate(lon2, lat2)));
        posts.add(postAtDistance);

        when(postRepository.findAll()).thenReturn(posts);

        // Using a radius large enough to include both posts
        List<Post> result = postService.getPostsWithinRadius(lat1, lon1, radius);

        // Then
        assertEquals(2, result.size()); // Both posts should be within radius
        assertTrue(result.contains(postAtOrigin));
        assertTrue(result.contains(postAtDistance));

        // Test with a smaller radius that should only include the origin post
        double smallRadius = 100.0; // km
        result = postService.getPostsWithinRadius(lat1, lon1, smallRadius);

        assertEquals(1, result.size());
        assertTrue(result.contains(postAtOrigin));
        assertFalse(result.contains(postAtDistance));
    }

    @Test
    void testCreatePost_Success() {
        // Given
        String title = "Test Post";
        String content = "This is a test post";
        Double latitude = 1.0;
        Double longitude = 2.0;
        String category = "safety";

        Post savedPost = new Post();
        savedPost.setTitle(title);
        savedPost.setCaption(content);
        savedPost.setCategory(category);
        savedPost.setCreatedAt(LocalDateTime.now()); // Use an actual LocalDateTime instead of a matcher
        savedPost.setLocation(geometryFactory.createPoint(new Coordinate(longitude, latitude)));

        when(postRepository.save(any(Post.class))).thenReturn(savedPost);

        // When
        Post result = postService.createPost(title, content, latitude, longitude, category);

        // Then
        assertNotNull(result);
        assertEquals(title, result.getTitle());
        assertEquals(content, result.getCaption());
        assertEquals(category, result.getCategory());
        verify(postRepository).save(any(Post.class));
    }
    
    @Test
    void testCreatePost_NullTitle() {
        // Given
        String title = null;
        String content = "This is a test post";
        Double latitude = 1.0;
        Double longitude = 2.0;
        String category = "safety";
        
        // When & Then
        assertThrows(InvalidPostDataException.class, () -> 
            postService.createPost(title, content, latitude, longitude, category)
        );
        verify(postRepository, never()).save(any(Post.class));
    }
    
    @Test
    void testCreatePost_EmptyTitle() {
        // Given
        String title = "   ";
        String content = "This is a test post";
        Double latitude = 1.0;
        Double longitude = 2.0;
        String category = "safety";
        
        // When & Then
        assertThrows(InvalidPostDataException.class, () -> 
            postService.createPost(title, content, latitude, longitude, category)
        );
        verify(postRepository, never()).save(any(Post.class));
    }
    
    @Test
    void testCreatePost_NullContent() {
        // Given
        String title = "Test Post";
        String content = null;
        Double latitude = 1.0;
        Double longitude = 2.0;
        String category = "safety";
        
        // When & Then
        assertThrows(InvalidPostDataException.class, () -> 
            postService.createPost(title, content, latitude, longitude, category)
        );
        verify(postRepository, never()).save(any(Post.class));
    }
    
    @Test
    void testCreatePost_EmptyContent() {
        // Given
        String title = "Test Post";
        String content = "  ";
        Double latitude = 1.0;
        Double longitude = 2.0;
        String category = "safety";
        
        // When & Then
        assertThrows(InvalidPostDataException.class, () -> 
            postService.createPost(title, content, latitude, longitude, category)
        );
        verify(postRepository, never()).save(any(Post.class));
    }
    
    @Test
    void testCreatePost_NullLatitude() {
        // Given
        String title = "Test Post";
        String content = "This is a test post";
        Double latitude = null;
        Double longitude = 2.0;
        String category = "safety";
        
        // When & Then
        assertThrows(InvalidPostDataException.class, () -> 
            postService.createPost(title, content, latitude, longitude, category)
        );
        verify(postRepository, never()).save(any(Post.class));
    }
    
    @Test
    void testCreatePost_NullLongitude() {
        // Given
        String title = "Test Post";
        String content = "This is a test post";
        Double latitude = 1.0;
        Double longitude = null;
        String category = "safety";
        
        // When & Then
        assertThrows(InvalidPostDataException.class, () -> 
            postService.createPost(title, content, latitude, longitude, category)
        );
        verify(postRepository, never()).save(any(Post.class));
    }
    
    @Test
    void testCreatePost_RepositoryException() {
        // Given
        String title = "Test Post";
        String content = "This is a test post";
        Double latitude = 1.0;
        Double longitude = 2.0;
        String category = "safety";
        
        when(postRepository.save(any(Post.class))).thenThrow(new RuntimeException("Database error"));
        
        // When & Then
        PostException exception = assertThrows(PostException.class, () -> 
            postService.createPost(title, content, latitude, longitude, category)
        );
        assertEquals("POST_CREATION_ERROR", exception.getErrorCode());
        assertTrue(exception.getMessage().contains("Failed to create post"));
    }

    @Test
    void testFilterPosts_WithCategoryStrategy() {
        // Given
        String category = "safety";
        List<Post> allPosts = Arrays.asList(post1, post2, post3);
        when(postRepository.findAll()).thenReturn(allPosts);
        
        CategoryFilteringStrategy categoryStrategy = new CategoryFilteringStrategy(category);
        
        // When
        List<Post> result = postService.filterPosts(categoryStrategy);
        
        // Then
        assertEquals(2, result.size());
        assertTrue(result.contains(post1));
        assertTrue(result.contains(post3));
        assertFalse(result.contains(post2));
    }
    
    @Test
    void testFilterPosts_WithDateRangeStrategy() {
        // Given
        LocalDateTime startDate = yesterday;
        LocalDateTime endDate = now;
        List<Post> allPosts = Arrays.asList(post1, post2, post3);
        when(postRepository.findAll()).thenReturn(allPosts);
        
        DateRangeFilteringStrategy dateRangeStrategy = new DateRangeFilteringStrategy(startDate, endDate);
        
        // When
        List<Post> result = postService.filterPosts(dateRangeStrategy);
        
        // Then
        assertEquals(2, result.size());
        assertTrue(result.contains(post1));
        assertTrue(result.contains(post2));
        assertFalse(result.contains(post3));
    }
    
    @Test
    void testFilterPosts_WithLocationRadiusStrategy() {
        // Given
        double centerLat = 0.1;
        double centerLon = 0.1;
        double radius = 5.0; // Smaller radius that only includes post1
        List<Post> allPosts = Arrays.asList(post1, post2, post3, postWithoutLocation);
        when(postRepository.findAll()).thenReturn(allPosts);
        
        LocationRadiusFilteringStrategy locationStrategy = new LocationRadiusFilteringStrategy(centerLat, centerLon, radius);
        
        // When
        List<Post> result = postService.filterPosts(locationStrategy);
        
        // Then
        assertEquals(1, result.size());
        assertTrue(result.contains(post1));
    }
    
    @Test
    void testFilterPosts_WithCompositeStrategy() {
        // Given
        double centerLat = 0.1;
        double centerLon = 0.1;
        double radius = 150.0; // Large radius
        String category = "safety";
        List<Post> allPosts = Arrays.asList(post1, post2, post3, postWithoutLocation);
        when(postRepository.findAll()).thenReturn(allPosts);
        
        CompositeFilteringStrategy compositeStrategy = new CompositeFilteringStrategy();
        compositeStrategy.addStrategy(new LocationRadiusFilteringStrategy(centerLat, centerLon, radius));
        compositeStrategy.addStrategy(new CategoryFilteringStrategy(category));
        
        // When
        List<Post> result = postService.filterPosts(compositeStrategy);
        
        // Then
        assertEquals(2, result.size());
        assertTrue(result.contains(post1));
        assertTrue(result.contains(post3));
        assertFalse(result.contains(post2));
        assertFalse(result.contains(postWithoutLocation));
    }

    @Test
    void testFindAllPosts() {
        // Given
        List<Post> allPosts = Arrays.asList(post1, post2, post3, postWithoutLocation);
        when(postRepository.findAll()).thenReturn(allPosts);

        // When
        List<Post> result = postService.findAll();

        // Then
        assertEquals(allPosts, result);
    }

    @Test
    void testFilterPostsByDate_NullDates(){
        // Given
        List<Post> allPosts = Arrays.asList(post1, post2, post3);
        when(postRepository.findAll()).thenReturn(allPosts);

        DateRangeFilteringStrategy dateRangeFilteringStrategy = new DateRangeFilteringStrategy(null, null);

        // When
        List<Post> result = postService.filterPosts(dateRangeFilteringStrategy);

        // Then
        assertEquals(3, result.size());
        assertTrue(result.contains(post1));
        assertTrue(result.contains(post2));
        assertTrue(result.contains(post3));
    }

    @Test
    void testFilterPostsByCategory_NullCategory(){
        // Given
        List<Post> allPosts = Arrays.asList(post1, post2, post3);
        when(postRepository.findAll()).thenReturn(allPosts);

        CategoryFilteringStrategy categoryFilteringStrategy = new CategoryFilteringStrategy(null);

        // When
        List<Post> result = postService.filterPosts(categoryFilteringStrategy);

        // Then
        assertEquals(3, result.size());
        assertTrue(result.contains(post1));
        assertTrue(result.contains(post2));
        assertTrue(result.contains(post3));
    }
}