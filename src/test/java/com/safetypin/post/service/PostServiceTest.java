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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.*;

@Slf4j
@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    private final Category
            safety = new Category("Safety"),
            crime = new Category("Crime");
    @Mock
    private PostRepository postRepository;    private final LocalDateTime
            now = LocalDateTime.now(),
            yesterday = now.minusDays(1),
            tomorrow = now.plusDays(1);
    private GeometryFactory geometryFactory;
    private PostServiceImpl postService;
    private Post post1, post2, post3;
    private Post postWithoutLocation;

    @BeforeEach
    void setup() {
        geometryFactory = new GeometryFactory();
        CategoryRepository categoryRepository = mock(CategoryRepository.class);
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

        // Update the mock to match the method actually called with null parameters
        when(postRepository.findPostsWithinPointAndRadius(
                any(), anyDouble(), any(PageRequest.class)
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

        // Update the mock to match the method actually called with null parameters
        when(postRepository.findPostsWithinPointAndRadius(
                any(), anyDouble(), any(PageRequest.class)
        )).thenReturn(postPage);

        // When
        Page<Map<String, Object>> result = postService.findPostsByLocation(
                centerLat, centerLon, radius, null, null, null, pageable);
        // Then
        assertEquals(1, result.getContent().size());
        assertNotNull(result.getContent().getFirst().get("distance"));
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
        savedPost.setCreatedAt(LocalDateTime.now()); // Use an actual LocalDateTime instead of a matcher
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



}