package com.safetypin.post.service;

import com.safetypin.post.exception.InvalidPostDataException;
import com.safetypin.post.exception.PostException;
import com.safetypin.post.model.Category;
import com.safetypin.post.model.Post;
import com.safetypin.post.repository.CategoryRepository;
import com.safetypin.post.repository.PostRepository;
import lombok.extern.slf4j.Slf4j;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@Slf4j
@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    private final Category
            safety = new Category("Safety"),
            crime = new Category("Crime");
    @Mock
    private PostRepository postRepository;
    @Mock
    private CategoryRepository categoryRepository;
    
    private final LocalDateTime
            now = LocalDateTime.now(),
            yesterday = now.minusDays(1),
            tomorrow = now.plusDays(1);
    private GeometryFactory geometryFactory;
    private PostServiceImpl postService;
    private Post post1, post2, post3;
    private Post postWithoutLocation;
    private Category safetyCategory;
    private Category crimeCategory;

    @BeforeEach
    void setup() {
        geometryFactory = new GeometryFactory();
        postService = new PostServiceImpl(postRepository, categoryRepository, geometryFactory);

        // Create test posts with locations
        post1 = new Post();
        post1.setLocation(geometryFactory.createPoint(new Coordinate(0.1, 0.1))); // lon, lat
        post1.setCategory(safety.getName());
        post1.setCreatedAt(now);

        post2 = new Post();
        post2.setLocation(geometryFactory.createPoint(new Coordinate(0.2, 0.2))); // lon, lat
        post2.setCategory(crime.getName());
        post2.setCreatedAt(yesterday);

        post3 = new Post();
        post3.setLocation(geometryFactory.createPoint(new Coordinate(0.3, 0.3))); // lon, lat
        post3.setCategory(safety.getName());
        post3.setCreatedAt(tomorrow);

        postWithoutLocation = new Post();
        postWithoutLocation.setCategory(safety.getName());
        postWithoutLocation.setCreatedAt(now);

        safetyCategory = new Category("Safety");
        safetyCategory.setId(UUID.randomUUID());

        crimeCategory = new Category("Crime");
        crimeCategory.setId(UUID.randomUUID());
    }

    @Test
    void testCreatePost_Success() {
        // Given
        String title = "Test Post";
        String content = "This is a test post";
        double latitude = 1.0;
        double longitude = 2.0;
        Category category = safety;

        Post savedPost = new Post();
        savedPost.setTitle(title);
        savedPost.setCaption(content);
        savedPost.setCategory(category.getName());
        savedPost.setCreatedAt(LocalDateTime.now());
        savedPost.setLocation(geometryFactory.createPoint(new Coordinate(longitude, latitude)));

        when(postRepository.save(any(Post.class))).thenReturn(savedPost);

        // When
        Post result = postService.createPost(title, content, latitude, longitude, category.getName());

        // Then
        assertNotNull(result);
        assertEquals(title, result.getTitle());
        assertEquals(content, result.getCaption());
        assertEquals(category.getName(), result.getCategory());
        verify(postRepository).save(any(Post.class));
    }

    @Test
    void testCreatePost_NullTitle() {
        // Given
        String title = null;
        String content = "This is a test post";
        Double latitude = 1.0;
        Double longitude = 2.0;
        Category category = safety;

        // When & Then
        assertThrows(InvalidPostDataException.class, () ->
                postService.createPost(title, content, latitude, longitude, category.getName())
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
        Category category = safety;

        // When & Then
        assertThrows(InvalidPostDataException.class, () ->
                postService.createPost(title, content, latitude, longitude, category.getName())
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
        Category category = safety;

        // When & Then
        assertThrows(InvalidPostDataException.class, () ->
                postService.createPost(title, content, latitude, longitude, category.getName())
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
        Category category = safety;

        when(postRepository.save(any(Post.class))).thenThrow(new RuntimeException("Database error"));

        // When & Then
        PostException exception = assertThrows(PostException.class, () ->
                postService.createPost(title, content, latitude, longitude, category.getName())
        );
        assertEquals("POST_ERROR", exception.getErrorCode());
        assertTrue(exception.getMessage().contains("Failed to save the post"));
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
    void testCreatePost_EmptyTitle() {
        // Given
        String title = "";
        String content = "This is a test post";
        Double latitude = 1.0;
        Double longitude = 2.0;
        Category category = safety;

        // When & Then
        InvalidPostDataException exception = assertThrows(InvalidPostDataException.class, () ->
                postService.createPost(title, content, latitude, longitude, category.getName())
        );
        assertEquals("Title is required", exception.getMessage());
        verify(postRepository, never()).save(any(Post.class));
    }

    @Test
    void testCreatePost_WhitespaceTitle() {
        // Given
        String title = "   ";
        String content = "This is a test post";
        Double latitude = 1.0;
        Double longitude = 2.0;
        Category category = safety;

        // When & Then
        InvalidPostDataException exception = assertThrows(InvalidPostDataException.class, () ->
                postService.createPost(title, content, latitude, longitude, category.getName())
        );
        assertEquals("Title is required", exception.getMessage());
        verify(postRepository, never()).save(any(Post.class));
    }

    @Test
    void testCreatePost_EmptyContent() {
        // Given
        String title = "Test Post";
        String content = "";
        Double latitude = 1.0;
        Double longitude = 2.0;
        Category category = safety;

        // When & Then
        InvalidPostDataException exception = assertThrows(InvalidPostDataException.class, () ->
                postService.createPost(title, content, latitude, longitude, category.getName())
        );
        assertEquals("Content is required", exception.getMessage());
        verify(postRepository, never()).save(any(Post.class));
    }

    @Test
    void testCreatePost_WhitespaceContent() {
        // Given
        String title = "Test Post";
        String content = "    ";
        Double latitude = 1.0;
        Double longitude = 2.0;
        Category category = safety;

        // When & Then
        InvalidPostDataException exception = assertThrows(InvalidPostDataException.class, () ->
                postService.createPost(title, content, latitude, longitude, category.getName())
        );
        assertEquals("Content is required", exception.getMessage());
        verify(postRepository, never()).save(any(Post.class));
    }

    @Test
    void testCreatePost_NullLongitude() {
        // Given
        String title = "Test Post";
        String content = "This is a test post";
        Double latitude = 1.0;
        Double longitude = null;
        Category category = safety;

        // When & Then
        InvalidPostDataException exception = assertThrows(InvalidPostDataException.class, () ->
                postService.createPost(title, content, latitude, longitude, category.getName())
        );
        assertEquals("Location coordinates are required", exception.getMessage());
        verify(postRepository, never()).save(any(Post.class));
    }

    @Test
    void testCreatePost_NullCategory() {
        // Given
        String title = "Test Post";
        String content = "This is a test post";
        Double latitude = 1.0;
        Double longitude = 2.0;
        String category = null;

        // When & Then
        InvalidPostDataException exception = assertThrows(InvalidPostDataException.class, () ->
                postService.createPost(title, content, latitude, longitude, category)
        );
        assertEquals("Category is required", exception.getMessage());
        verify(postRepository, never()).save(any(Post.class));
    }

    @Test
    void testCreatePost_EmptyCategory() {
        // Given
        String title = "Test Post";
        String content = "This is a test post";
        Double latitude = 1.0;
        Double longitude = 2.0;
        String category = "";

        // When & Then
        InvalidPostDataException exception = assertThrows(InvalidPostDataException.class, () ->
                postService.createPost(title, content, latitude, longitude, category)
        );
        assertEquals("Category is required", exception.getMessage());
        verify(postRepository, never()).save(any(Post.class));
    }

    @Test
    void testCreatePost_WhitespaceCategory() {
        // Given
        String title = "Test Post";
        String content = "This is a test post";
        Double latitude = 1.0;
        Double longitude = 2.0;
        String category = "   ";

        // When & Then
        InvalidPostDataException exception = assertThrows(InvalidPostDataException.class, () ->
                postService.createPost(title, content, latitude, longitude, category)
        );
        assertEquals("Category is required", exception.getMessage());
        verify(postRepository, never()).save(any(Post.class));
    }

    @Test
    void testCreatePost_ValidBoundaryValues() {
        // Given - Neutral case with minimal valid values
        String title = "X";  // Minimal valid title
        String content = "Y"; // Minimal valid content
        Double latitude = 90.0; // Boundary value (max valid latitude)
        Double longitude = 180.0; // Boundary value (max valid longitude)
        Category category = safety;

        Post savedPost = new Post();
        savedPost.setTitle(title);
        savedPost.setCaption(content);
        savedPost.setCategory(category.getName());
        savedPost.setLocation(geometryFactory.createPoint(new Coordinate(longitude, latitude)));

        when(postRepository.save(any(Post.class))).thenReturn(savedPost);

        // When
        Post result = postService.createPost(title, content, latitude, longitude, category.getName());

        // Then
        assertNotNull(result);
        assertEquals(title, result.getTitle());
        assertEquals(content, result.getCaption());
        assertEquals(category.getName(), result.getCategory());
        verify(postRepository).save(any(Post.class));
    }

    // Test findPostsByLocation with null repository response
    @Test
    void testFindPostsByLocationWithNullResponse() {
        // Given
        double centerLat = 0.15;
        double centerLon = 0.15;
        double radius = 20.0; // 20 km
        Pageable pageable = PageRequest.of(0, 10);

        when(postRepository.findPostsWithinRadius(any(Point.class), anyDouble(), eq(pageable)))
                .thenReturn(null);

        // When
        Page<Map<String, Object>> result = postService.findPostsByLocation(
                centerLat, centerLon, radius, null, null, null, pageable);

        // Then
        assertNotNull(result);
        assertTrue(result.getContent().isEmpty());
    }

    // Test findPostsByLocation with category filter
    @Test
    void testFindPostsByLocationWithCategoryFilter() {
        // Given
        double centerLat = 0.15;
        double centerLon = 0.15;
        double radius = 20.0; // 20 km
        String category = "Crime";
        Pageable pageable = PageRequest.of(0, 10);

        List<Post> posts = Arrays.asList(post1, post2, post3);
        Page<Post> postsPage = new PageImpl<>(posts, pageable, posts.size());

        when(postRepository.findPostsWithinRadius(any(Point.class), anyDouble(), eq(pageable)))
                .thenReturn(postsPage);

        // When
        Page<Map<String, Object>> result = postService.findPostsByLocation(
                centerLat, centerLon, radius, category, null, null, pageable);

        // Then
        assertNotNull(result);
        // Only post2 has Crime category
        assertEquals(1, result.getContent().size());
    }

    // Test findPostsByLocation with distance filtering
    @Test
    void testFindPostsByLocationWithDistanceFiltering() {
        // Given
        double centerLat = 0.1;  // Same as post1's latitude
        double centerLon = 0.1;  // Same as post1's longitude
        double radius = 5.0;     // Small radius to filter out post2 and post3
        Pageable pageable = PageRequest.of(0, 10);

        List<Post> posts = Arrays.asList(post1, post2, post3);
        Page<Post> postsPage = new PageImpl<>(posts, pageable, posts.size());

        when(postRepository.findPostsWithinRadius(any(Point.class), anyDouble(), eq(pageable)))
                .thenReturn(postsPage);

        // When
        Page<Map<String, Object>> result = postService.findPostsByLocation(
                centerLat, centerLon, radius, null, null, null, pageable);

        // Then
        assertNotNull(result);
        // Only post1 should be within the small radius
        assertEquals(1, result.getContent().size());

        Map<String, Object> postResult = result.getContent().get(0);
        Map<String, Object> postData = (Map<String, Object>) postResult.get("post");
        assertEquals(0.0, postResult.get("distance"));
        assertEquals(post1.getCategory(), postData.get("category"));
    }

    // Test category UUID handling in findPostsByLocation
    @Test
    void testFindPostsByLocationWithCategoryUUID() {
        // Given
        double centerLat = 0.15;
        double centerLon = 0.15;
        double radius = 20.0;
        Pageable pageable = PageRequest.of(0, 10);

        // Create a post with UUID as category
        Post postWithCategoryId = new Post();
        postWithCategoryId.setLocation(geometryFactory.createPoint(new Coordinate(0.1, 0.1)));
        postWithCategoryId.setCategory(safetyCategory.getId().toString());
        postWithCategoryId.setTitle("Safety Post");
        postWithCategoryId.setCreatedAt(now);

        List<Post> posts = Collections.singletonList(postWithCategoryId);
        Page<Post> postsPage = new PageImpl<>(posts, pageable, posts.size());

        when(postRepository.findPostsWithinRadius(any(Point.class), anyDouble(), eq(pageable)))
                .thenReturn(postsPage);
                
        when(categoryRepository.findById(safetyCategory.getId()))
                .thenReturn(Optional.of(safetyCategory));

        // When
        Page<Map<String, Object>> result = postService.findPostsByLocation(
                centerLat, centerLon, radius, "Safety", null, null, pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        Map<String, Object> postData = (Map<String, Object>) result.getContent().get(0).get("post");
        assertEquals("Safety", postData.get("category"));
    }

    // Test invalid UUID handling in findPostsByLocation
    @Test
    void testFindPostsByLocationWithInvalidCategoryUUID() {
        // Given
        double centerLat = 0.15;
        double centerLon = 0.15;
        double radius = 20.0;
        Pageable pageable = PageRequest.of(0, 10);

        // Create a post with invalid UUID format as category
        Post postWithInvalidCategoryId = new Post();
        postWithInvalidCategoryId.setLocation(geometryFactory.createPoint(new Coordinate(0.1, 0.1)));
        postWithInvalidCategoryId.setCategory("not-a-uuid");
        postWithInvalidCategoryId.setTitle("Invalid Category Post");
        postWithInvalidCategoryId.setCreatedAt(now);

        List<Post> posts = Collections.singletonList(postWithInvalidCategoryId);
        Page<Post> postsPage = new PageImpl<>(posts, pageable, posts.size());

        when(postRepository.findPostsWithinRadius(any(Point.class), anyDouble(), eq(pageable)))
                .thenReturn(postsPage);

        // When
        Page<Map<String, Object>> result = postService.findPostsByLocation(
                centerLat, centerLon, radius, "not-a-uuid", null, null, pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        Map<String, Object> postData = (Map<String, Object>) result.getContent().get(0).get("post");
        assertEquals("not-a-uuid", postData.get("category"));
    }

}