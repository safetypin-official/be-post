package com.safetypin.post.service;

import com.safetypin.post.exception.InvalidPostDataException;
import com.safetypin.post.exception.PostException;
import com.safetypin.post.exception.PostNotFoundException;
import com.safetypin.post.model.Category;
import com.safetypin.post.model.Post;
import com.safetypin.post.repository.CategoryRepository;
import com.safetypin.post.repository.PostRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
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
    private GeometryFactory geometryFactory;
    private PostServiceImpl postService;    private final LocalDateTime
            now = LocalDateTime.now(),
            yesterday = now.minusDays(1),
            tomorrow = now.plusDays(1);
    private Post post1, post2, post3;
    private Post postWithoutLocation;
    private Category safetyCategory;

    /**
     * Arguments provider for title and content validation tests
     */
    private static Stream<Arguments> titleAndContentValidationProvider() {
        return Stream.of(
                // title, content, fieldName, expectedMessage
                Arguments.of(null, "Valid content", "title", "Title is required"),
                Arguments.of("", "Valid content", "title", "Title is required"),
                Arguments.of("   ", "Valid content", "title", "Title is required"),
                Arguments.of("Valid title", null, "content", "Content is required"),
                Arguments.of("Valid title", "", "content", "Content is required"),
                Arguments.of("Valid title", "   ", "content", "Content is required")
        );
    }

    /**
     * Arguments provider for category validation tests
     */
    private static Stream<Arguments> categoryValidationProvider() {
        return Stream.of(
                // categoryName, expectedMessage
                Arguments.of(null, "Category is required"),
                Arguments.of("", "Category is required"),
                Arguments.of("   ", "Category is required")
        );
    }

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
    }

    @ParameterizedTest
    @MethodSource("titleAndContentValidationProvider")
    void testCreatePostTitleAndContentValidation(String title, String content, String fieldName, String expectedMessage) {
        // Given
        Double latitude = 1.0;
        Double longitude = 2.0;
        Category category = safety;

        // When & Then
        Executable executable = () -> postService.createPost(title, content, latitude, longitude, category.getName());
        InvalidPostDataException exception = assertThrows(InvalidPostDataException.class, executable);
        assertEquals(expectedMessage, exception.getMessage());
        verify(postRepository, never()).save(any(Post.class));
    }

    @ParameterizedTest
    @MethodSource("categoryValidationProvider")
    void testCreatePostCategoryValidation(String categoryName, String expectedMessage) {
        // Given
        String title = "Test Post";
        String content = "This is a test post";
        Double latitude = 1.0;
        Double longitude = 2.0;

        // When & Then
        InvalidPostDataException exception = assertThrows(InvalidPostDataException.class, () ->
                postService.createPost(title, content, latitude, longitude, categoryName)
        );
        assertEquals(expectedMessage, exception.getMessage());
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
        Executable executable = () -> postService.createPost(title, content, latitude, longitude, category.getName());
        InvalidPostDataException exception = assertThrows(InvalidPostDataException.class, executable);
        assertEquals("Location coordinates are required", exception.getMessage());
        verify(postRepository, never()).save(any(Post.class));
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
    void testCreatePost_NullLongitude() {
        // Given
        String title = "Test Post";
        String content = "This is a test post";
        Double latitude = 1.0;
        Double longitude = null;
        Category category = safety;

        // When & Then
        Executable executable = () -> postService.createPost(title, content, latitude, longitude, category.getName());
        InvalidPostDataException exception = assertThrows(InvalidPostDataException.class, executable);
        assertEquals("Location coordinates are required", exception.getMessage());
        verify(postRepository, never()).save(any(Post.class));
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

    @Test
    void testFindAllPaginated() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        List<Post> posts = Arrays.asList(post1, post2, post3);
        Page<Post> expectedPage = new PageImpl<>(posts, pageable, posts.size());

        when(postRepository.findAll(pageable)).thenReturn(expectedPage);

        // When
        Page<Post> result = postService.findAllPaginated(pageable);

        // Then
        assertEquals(expectedPage, result);
        verify(postRepository).findAll(pageable);
    }

    @Test
    void testCreatePost_Success() {
        // Given
        String title = "Test Post";
        String content = "This is a test post";
        Double latitude = 1.0;
        Double longitude = 2.0;
        String categoryName = "Safety";

        Post expectedPost = new Post();
        expectedPost.setTitle(title);
        expectedPost.setCaption(content);
        expectedPost.setLocation(geometryFactory.createPoint(new Coordinate(longitude, latitude)));
        expectedPost.setCategory(categoryName);

        when(categoryRepository.findByName(categoryName)).thenReturn(safetyCategory);
        when(postRepository.save(any(Post.class))).thenReturn(expectedPost);

        // When
        Post result = postService.createPost(title, content, latitude, longitude, categoryName);

        // Then
        assertNotNull(result);
        assertEquals(title, result.getTitle());
        assertEquals(content, result.getCaption());
        assertEquals(categoryName, result.getCategory());

        verify(categoryRepository).findByName(categoryName);
        verify(postRepository).save(any(Post.class));
    }

    // Test category UUID handling in findPostsByLocation

    @Test
    void testCreatePost_NonExistentCategory() {
        // Given
        String title = "Test Post";
        String content = "This is a test post";
        Double latitude = 1.0;
        Double longitude = 2.0;
        String categoryName = "NonExistentCategory";

        when(categoryRepository.findByName(categoryName)).thenReturn(null);

        // When & Then
        InvalidPostDataException exception = assertThrows(InvalidPostDataException.class, () ->
                postService.createPost(title, content, latitude, longitude, categoryName)
        );

        assertEquals("Category does not exist: " + categoryName, exception.getMessage());
        verify(categoryRepository).findByName(categoryName);
        verify(postRepository, never()).save(any(Post.class));
    }

    @Test
    void testCreatePost_RepositoryException() {
        // Given
        String title = "Test Post";
        String content = "This is a test post";
        Double latitude = 1.0;
        Double longitude = 2.0;
        String categoryName = "Safety";

        when(categoryRepository.findByName(categoryName)).thenReturn(safetyCategory);
        when(postRepository.save(any(Post.class))).thenThrow(new RuntimeException("Database error"));

        // When & Then
        PostException exception = assertThrows(PostException.class, () ->
                postService.createPost(title, content, latitude, longitude, categoryName)
        );

        assertTrue(exception.getMessage().contains("Failed to save the post"));
        verify(categoryRepository).findByName(categoryName);
        verify(postRepository).save(any(Post.class));
    }

    @Test
    void testFindPostsByLocationWithDateFilters() {
        // Given
        double centerLat = 0.15;
        double centerLon = 0.15;
        double radius = 20.0; // 20 km
        LocalDateTime dateFrom = yesterday;
        LocalDateTime dateTo = tomorrow;
        Pageable pageable = PageRequest.of(0, 10);

        List<Post> posts = Arrays.asList(post1, post2);
        Page<Post> postsPage = new PageImpl<>(posts, pageable, posts.size());

        when(postRepository.findPostsByDateRange(any(Point.class), anyDouble(), eq(dateFrom), eq(dateTo), eq(pageable)))
                .thenReturn(postsPage);

        // When
        Page<Map<String, Object>> result = postService.findPostsByLocation(
                centerLat, centerLon, radius, null, dateFrom, dateTo, pageable);

        // Then
        assertNotNull(result);
        assertEquals(2, result.getContent().size());
        verify(postRepository).findPostsByDateRange(any(Point.class), anyDouble(), eq(dateFrom), eq(dateTo), eq(pageable));
    }

    @Test
    void testFindPostsByLocationWithPostWithoutLocation() {
        // Given
        double centerLat = 0.15;
        double centerLon = 0.15;
        double radius = 20.0; // 20 km
        Pageable pageable = PageRequest.of(0, 10);

        List<Post> posts = Collections.singletonList(postWithoutLocation);
        Page<Post> postsPage = new PageImpl<>(posts, pageable, posts.size());

        when(postRepository.findPostsWithinRadius(any(Point.class), anyDouble(), eq(pageable)))
                .thenReturn(postsPage);

        // When
        Page<Map<String, Object>> result = postService.findPostsByLocation(
                centerLat, centerLon, radius, null, null, null, pageable);

        // Then
        assertNotNull(result);
        // Post without location should be filtered out because distance check would fail
        assertEquals(0, result.getContent().size());
    }

    @Test
    void testFindPostsByLocationWithDateFiltersNull() {
        // Given
        double centerLat = 0.15;
        double centerLon = 0.15;
        double radius = 20.0; // 20 km


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
        assertEquals(2, result.getContent().size());
        verify(postRepository).findPostsWithinRadius(any(Point.class), anyDouble(), eq(pageable));
    }

    @Test
    void testFindPostsByDistanceFeed_SuccessWithSorting() {
        // Given
        Double userLat = 0.0;
        Double userLon = 0.0;
        Pageable pageable = PageRequest.of(0, 10);

        // Create posts at different distances
        Post nearPost = new Post();
        nearPost.setLocation(geometryFactory.createPoint(new Coordinate(0.01, 0.01))); // very close
        nearPost.setLatitude(0.01);
        nearPost.setLongitude(0.01);
        nearPost.setCategory("Safety");

        Post midPost = new Post();
        midPost.setLocation(geometryFactory.createPoint(new Coordinate(0.05, 0.05))); // medium distance
        midPost.setLatitude(0.05);
        midPost.setLongitude(0.05);
        midPost.setCategory("Crime");

        Post farPost = new Post();
        farPost.setLocation(geometryFactory.createPoint(new Coordinate(0.1, 0.1))); // furthest
        farPost.setLatitude(0.1);
        farPost.setLongitude(0.1);
        farPost.setCategory("Safety");

        List<Post> allPosts = Arrays.asList(farPost, midPost, nearPost); // Intentionally unsorted

        when(postRepository.findAll()).thenReturn(allPosts);

        // When
        Page<Map<String, Object>> result = postService.findPostsByDistanceFeed(userLat, userLon, pageable);

        // Then
        assertNotNull(result);
        assertEquals(3, result.getContent().size());

        // Verify sorting (nearest first)
        double firstDistance = (Double) result.getContent().get(0).get("distance");
        double secondDistance = (Double) result.getContent().get(1).get("distance");
        double thirdDistance = (Double) result.getContent().get(2).get("distance");

        assertTrue(firstDistance < secondDistance);
        assertTrue(secondDistance < thirdDistance);

        // Verify content is correctly mapped
        Map<String, Object> firstPost = (Map<String, Object>) result.getContent().get(0).get("post");
        assertNotNull(firstPost);
        assertEquals("Safety", firstPost.get("category"));
        assertEquals(0.01, firstPost.get("latitude"));
    }

    @Test
    void testFindPostsByDistanceFeed_EmptyResult() {
        // Given
        Double userLat = 0.0;
        Double userLon = 0.0;
        Pageable pageable = PageRequest.of(0, 10);

        when(postRepository.findAll()).thenReturn(Collections.emptyList());

        // When
        Page<Map<String, Object>> result = postService.findPostsByDistanceFeed(userLat, userLon, pageable);

        // Then
        assertNotNull(result);
        assertTrue(result.getContent().isEmpty());
        assertEquals(0, result.getTotalElements());
    }

    @Test
    void testFindPostsByDistanceFeed_Pagination() {
        // Given
        Double userLat = 0.0;
        Double userLon = 0.0;
        int pageSize = 2;

        // Create multiple posts
        List<Post> allPosts = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            Post post = new Post();
            post.setLocation(geometryFactory.createPoint(new Coordinate(0.01 * i, 0.01 * i)));
            post.setLatitude(0.01 * i);
            post.setLongitude(0.01 * i);
            post.setCategory("Category " + i);
            allPosts.add(post);
        }

        when(postRepository.findAll()).thenReturn(allPosts);

        // When - First page
        Page<Map<String, Object>> firstPageResult = postService.findPostsByDistanceFeed(
                userLat, userLon, PageRequest.of(0, pageSize));

        // Then - First page
        assertEquals(2, firstPageResult.getContent().size());
        assertEquals(5, firstPageResult.getTotalElements());
        assertEquals(3, firstPageResult.getTotalPages());
        assertTrue(firstPageResult.hasNext());
        assertFalse(firstPageResult.hasPrevious());

        // When - Second page
        Page<Map<String, Object>> secondPageResult = postService.findPostsByDistanceFeed(
                userLat, userLon, PageRequest.of(1, pageSize));

        // Then - Second page
        assertEquals(2, secondPageResult.getContent().size());
        assertEquals(5, secondPageResult.getTotalElements());
        assertEquals(1, secondPageResult.getNumber());
        assertTrue(secondPageResult.hasNext());
        assertTrue(secondPageResult.hasPrevious());
    }

    @Test
    void testFindPostsByDistanceFeed_PaginationBeyondAvailableData() {
        // Given
        Double userLat = 0.0;
        Double userLon = 0.0;
        Pageable pageable = PageRequest.of(5, 10); // Page beyond available data

        List<Post> allPosts = Arrays.asList(post1, post2, post3); // Just 3 posts

        when(postRepository.findAll()).thenReturn(allPosts);

        // When
        Page<Map<String, Object>> result = postService.findPostsByDistanceFeed(userLat, userLon, pageable);

        // Then
        assertNotNull(result);
        assertTrue(result.getContent().isEmpty());
        assertEquals(3, result.getTotalElements());
        assertEquals(1, result.getTotalPages());
        assertFalse(result.hasNext());
        assertTrue(result.hasPrevious());
    }

    @Test
    void testFindPostsByTimestampFeed_Success() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        List<Post> posts = Arrays.asList(post1, post2, post3);
        Page<Post> postPage = new PageImpl<>(posts, pageable, posts.size());

        when(postRepository.findAll(pageable)).thenReturn(postPage);

        // When
        Page<Map<String, Object>> result = postService.findPostsByTimestampFeed(pageable);

        // Then
        assertNotNull(result);
        assertEquals(3, result.getContent().size());
        verify(postRepository).findAll(pageable);

        // Verify sorting (newest first)
        List<Map<String, Object>> resultContent = result.getContent();

        // Check that the first post has the most recent date (post3 has tomorrow date)
        Map<String, Object> firstPostMap = resultContent.get(0);
        Map<String, Object> firstPostData = (Map<String, Object>) firstPostMap.get("post");
        assertEquals(post3.getCreatedAt(), firstPostData.get("createdAt"));

        // Check that the last post has the oldest date (post2 has yesterday date)
        Map<String, Object> lastPostMap = resultContent.get(2);
        Map<String, Object> lastPostData = (Map<String, Object>) lastPostMap.get("post");
        assertEquals(post2.getCreatedAt(), lastPostData.get("createdAt"));
    }

    @Test
    void testFindPostsByTimestampFeed_EmptyResult() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<Post> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);

        when(postRepository.findAll(pageable)).thenReturn(emptyPage);

        // When
        Page<Map<String, Object>> result = postService.findPostsByTimestampFeed(pageable);

        // Then
        assertNotNull(result);
        assertTrue(result.getContent().isEmpty());
        assertEquals(0, result.getTotalElements());
        verify(postRepository).findAll(pageable);
    }

    @Test
    void testFindPostsByTimestampFeed_Pagination() {
        // Given
        int pageSize = 2;
        Pageable firstPageable = PageRequest.of(0, pageSize);

        List<Post> allPosts = Arrays.asList(post1, post2, post3);
        Page<Post> firstPage = new PageImpl<>(
                allPosts.subList(0, 2), firstPageable, allPosts.size());

        when(postRepository.findAll(firstPageable)).thenReturn(firstPage);

        // When
        Page<Map<String, Object>> result = postService.findPostsByTimestampFeed(firstPageable);

        // Then
        assertNotNull(result);
        assertEquals(2, result.getContent().size());
        assertEquals(3, result.getTotalElements());
        assertEquals(2, result.getTotalPages());
        assertTrue(result.hasNext());
        assertFalse(result.hasPrevious());
        verify(postRepository).findAll(firstPageable);
    }

    // POSITIVE TEST CASES FOR SEARCH POSTS

    @Test
    void testSearchPosts_WithKeywordOnly() {
        // Given
        Double centerLat = 0.15;
        Double centerLon = 0.15;
        Double radius = 20.0; // 20 km
        String keyword = "test";
        List<String> categories = null;
        Pageable pageable = PageRequest.of(0, 10);

        List<Post> posts = Arrays.asList(post1, post2);
        Page<Post> postsPage = new PageImpl<>(posts, pageable, posts.size());

        when(postRepository.searchPostsByKeyword(any(Point.class), anyDouble(), eq(keyword), eq(pageable)))
                .thenReturn(postsPage);

        // When
        Page<Map<String, Object>> result = postService.searchPosts(
                centerLat, centerLon, radius, keyword, categories, pageable);

        // Then
        assertNotNull(result);
        assertEquals(2, result.getContent().size());
        verify(postRepository).searchPostsByKeyword(any(Point.class), anyDouble(), eq(keyword), eq(pageable));
    }

    @Test
    void testSearchPosts_WithCategoriesOnly() {
        // Given
        Double centerLat = 0.15;
        Double centerLon = 0.15;
        Double radius = 20.0; // 20 km
        String keyword = null;
        List<String> categories = Arrays.asList("Safety", "Crime");
        Pageable pageable = PageRequest.of(0, 10);

        List<Post> posts = Arrays.asList(post1, post3);
        Page<Post> postsPage = new PageImpl<>(posts, pageable, posts.size());

        when(categoryRepository.findByName("Safety")).thenReturn(safetyCategory);
        when(categoryRepository.findByName("Crime")).thenReturn(crime);
        when(postRepository.searchPostsByCategories(any(Point.class), anyDouble(), eq(categories), eq(pageable)))
                .thenReturn(postsPage);

        // When
        Page<Map<String, Object>> result = postService.searchPosts(
                centerLat, centerLon, radius, keyword, categories, pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        verify(postRepository).searchPostsByCategories(any(Point.class), anyDouble(), eq(categories), eq(pageable));
        verify(categoryRepository, times(2)).findByName(anyString());
    }

    @Test
    void testSearchPosts_WithKeywordAndCategories() {
        // Given
        Double centerLat = 0.15;
        Double centerLon = 0.15;
        Double radius = 20.0; // 20 km
        String keyword = "test";
        List<String> categories = Arrays.asList("Safety");
        Pageable pageable = PageRequest.of(0, 10);

        List<Post> posts = Collections.singletonList(post1);
        Page<Post> postsPage = new PageImpl<>(posts, pageable, posts.size());

        when(categoryRepository.findByName("Safety")).thenReturn(safetyCategory);
        when(postRepository.searchPostsByKeywordAndCategories(any(Point.class), anyDouble(), eq(keyword), eq(categories), eq(pageable)))
                .thenReturn(postsPage);

        // When
        Page<Map<String, Object>> result = postService.searchPosts(
                centerLat, centerLon, radius, keyword, categories, pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        verify(postRepository).searchPostsByKeywordAndCategories(any(Point.class), anyDouble(), eq(keyword), eq(categories), eq(pageable));
        verify(categoryRepository).findByName("Safety");
    }

    @Test
    void testSearchPosts_NoSearchCriteria() {
        // Given
        Double centerLat = 0.15;
        Double centerLon = 0.15;
        Double radius = 20.0; // 20 km
        String keyword = "";  // Empty keyword
        List<String> categories = Collections.emptyList(); // Empty categories
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Map<String, Object>> result = postService.searchPosts(
                centerLat, centerLon, radius, keyword, categories, pageable);

        // Then
        assertNotNull(result);
        assertTrue(result.getContent().isEmpty());
        assertEquals(0, result.getTotalElements());
        
        // Verify no repository methods were called
        verifyNoInteractions(postRepository);
    }

    @Test
    void testSearchPosts_NullResultFromRepository() {
        // Given
        Double centerLat = 0.15;
        Double centerLon = 0.15;
        Double radius = 20.0; // 20 km
        String keyword = "test";
        List<String> categories = null;
        Pageable pageable = PageRequest.of(0, 10);

        when(postRepository.searchPostsByKeyword(any(Point.class), anyDouble(), eq(keyword), eq(pageable)))
                .thenReturn(null);

        // When
        Page<Map<String, Object>> result = postService.searchPosts(
                centerLat, centerLon, radius, keyword, categories, pageable);

        // Then
        assertNotNull(result);
        assertTrue(result.getContent().isEmpty());
        assertEquals(0, result.getTotalElements());
    }

    @Test
    void testSearchPosts_DistanceFiltering() {
        // Given
        Double centerLat = 0.1;
        Double centerLon = 0.1;
        Double radius = 5.0; // Small radius to filter out distant posts
        String keyword = "test";
        List<String> categories = null;
        Pageable pageable = PageRequest.of(0, 10);

        // post1 is close, post2 and post3 are farther away
        List<Post> posts = Arrays.asList(post1, post2, post3);
        Page<Post> postsPage = new PageImpl<>(posts, pageable, posts.size());

        when(postRepository.searchPostsByKeyword(any(Point.class), anyDouble(), eq(keyword), eq(pageable)))
                .thenReturn(postsPage);

        // When
        Page<Map<String, Object>> result = postService.searchPosts(
                centerLat, centerLon, radius, keyword, categories, pageable);

        // Then
        assertNotNull(result);
        // Only post1 should be within the small radius
        assertEquals(1, result.getContent().size());
        
        Map<String, Object> postResult = result.getContent().get(0);
        Map<String, Object> postData = (Map<String, Object>) postResult.get("post");
        assertEquals(post1.getCategory(), postData.get("category"));
    }

    @Test
    void testSearchPosts_Pagination() {
        // Given
        Double centerLat = 0.15;
        Double centerLon = 0.15;
        Double radius = 20.0; // 20 km
        String keyword = "test";
        List<String> categories = null;
        int pageSize = 2;
        Pageable pageable = PageRequest.of(0, pageSize);

        // Create posts to fill multiple pages
        List<Post> posts = Arrays.asList(post1, post2);
        Page<Post> postsPage = new PageImpl<>(posts, pageable, 5); // 5 total elements but only 2 in this page

        when(postRepository.searchPostsByKeyword(any(Point.class), anyDouble(), eq(keyword), eq(pageable)))
                .thenReturn(postsPage);

        // When
        Page<Map<String, Object>> result = postService.searchPosts(
                centerLat, centerLon, radius, keyword, categories, pageable);

        // Then
        assertNotNull(result);
        assertEquals(2, result.getContent().size());
        assertEquals(2, result.getTotalElements()); // This will be the size of the filtered results
        assertEquals(1, result.getTotalPages());
    }

    // NEGATIVE TEST CASES FOR SEARCH POSTS

    @Test
    void testSearchPosts_NonExistentCategory() {
        // Given
        Double centerLat = 0.15;
        Double centerLon = 0.15;
        Double radius = 20.0; // 20 km
        String keyword = null;
        List<String> categories = Arrays.asList("Safety", "NonExistentCategory");
        Pageable pageable = PageRequest.of(0, 10);

        when(categoryRepository.findByName("Safety")).thenReturn(safetyCategory);
        when(categoryRepository.findByName("NonExistentCategory")).thenReturn(null);

        // When & Then
        InvalidPostDataException exception = assertThrows(InvalidPostDataException.class, () ->
                postService.searchPosts(centerLat, centerLon, radius, keyword, categories, pageable)
        );

        assertEquals("Category does not exist: NonExistentCategory", exception.getMessage());
        verify(categoryRepository, times(2)).findByName(anyString());
        verify(postRepository, never()).searchPostsByCategories(any(), anyDouble(), any(), any());
    }

    @Test
    void testSearchPosts_RepositoryException() {
        // Given
        Double centerLat = 0.15;
        Double centerLon = 0.15;
        Double radius = 20.0; // 20 km
        String keyword = "test";
        List<String> categories = null;
        Pageable pageable = PageRequest.of(0, 10);

        when(postRepository.searchPostsByKeyword(any(Point.class), anyDouble(), eq(keyword), eq(pageable)))
                .thenThrow(new RuntimeException("Database error"));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                postService.searchPosts(centerLat, centerLon, radius, keyword, categories, pageable)
        );

        assertEquals("Database error", exception.getMessage());
    }

    @Test
    void testSearchPosts_EmptyCategoriesList() {
        // Given
        Double centerLat = 0.15;
        Double centerLon = 0.15;
        Double radius = 20.0; // 20 km
        String keyword = "test";
        List<String> categories = Collections.emptyList();
        Pageable pageable = PageRequest.of(0, 10);

        List<Post> posts = Arrays.asList(post1, post2);
        Page<Post> postsPage = new PageImpl<>(posts, pageable, posts.size());

        when(postRepository.searchPostsByKeyword(any(Point.class), anyDouble(), eq(keyword), eq(pageable)))
                .thenReturn(postsPage);

        // When
        Page<Map<String, Object>> result = postService.searchPosts(
                centerLat, centerLon, radius, keyword, categories, pageable);

        // Then
        assertNotNull(result);
        assertEquals(2, result.getContent().size());
        verify(postRepository).searchPostsByKeyword(any(Point.class), anyDouble(), eq(keyword), eq(pageable));
        // No category validation should happen
        verifyNoInteractions(categoryRepository);
    }

    @Test
    void testSearchPosts_WhitespaceKeyword() {
        // Given
        Double centerLat = 0.15;
        Double centerLon = 0.15;
        Double radius = 20.0; // 20 km
        String keyword = "   "; // Just whitespace
        List<String> categories = null;
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Map<String, Object>> result = postService.searchPosts(
                centerLat, centerLon, radius, keyword, categories, pageable);

        // Then
        assertNotNull(result);
        assertTrue(result.getContent().isEmpty());
        // Should be treated as no search criteria provided
        verifyNoInteractions(postRepository);
    }

    @Test
    void testFindById_Success() {
        // Given
        UUID id = UUID.randomUUID();
        post1.setId(id);
        
        when(postRepository.findById(id)).thenReturn(Optional.of(post1));
        
        // When
        Post result = postService.findById(id);
        
        // Then
        assertNotNull(result);
        assertEquals(id, result.getId());
        assertEquals(post1.getCategory(), result.getCategory());
        verify(postRepository).findById(id);
    }
    
    @Test
    void testFindById_NotFound() {
        // Given
        UUID id = UUID.randomUUID();
        
        when(postRepository.findById(id)).thenReturn(Optional.empty());
        
        // When & Then
        PostNotFoundException exception = assertThrows(PostNotFoundException.class, () ->
            postService.findById(id)
        );
        
        // Verify exception message contains the ID
        assertTrue(exception.getMessage().contains(id.toString()));
        verify(postRepository).findById(id);
    }

    @Test
    void testFindPostsByLocationWithNullLocation() {
        // Given
        double centerLat = 0.15;
        double centerLon = 0.15;
        double radius = 20.0; // 20 km
        Pageable pageable = PageRequest.of(0, 10);

        // Create a post with null location for this test
        Post postWithNullLocation = new Post();
        postWithNullLocation.setLocation(null); // Force null location
        postWithNullLocation.setCategory(safety.getName());
        postWithNullLocation.setCreatedAt(now);

        List<Post> posts = Collections.singletonList(postWithNullLocation);
        Page<Post> postsPage = new PageImpl<>(posts, pageable, posts.size());

        when(postRepository.findPostsWithinRadius(any(Point.class), anyDouble(), eq(pageable)))
                .thenReturn(postsPage);

        // When
        Page<Map<String, Object>> result = postService.findPostsByLocation(
                centerLat, centerLon, radius, null, null, null, pageable);

        // Then
        assertNotNull(result);
        // The post should be filtered out during distance calculation/filtering
        assertTrue(result.getContent().isEmpty());
        
        // Since null location results in distance = 0.0, verify post processing
        verify(postRepository).findPostsWithinRadius(any(Point.class), anyDouble(), eq(pageable));
    }

    @Test
    void testFindPostsByLocation_PostObjectNotMap() {
        // Given
        double centerLat = 0.15;
        double centerLon = 0.15;
        double radius = 20.0; // 20 km
        Pageable pageable = PageRequest.of(0, 10);

        // Create a post with location
        Post validPost = new Post();
        validPost.setLocation(geometryFactory.createPoint(new Coordinate(0.1, 0.1)));
        validPost.setLatitude(0.1);
        validPost.setLongitude(0.1);
        validPost.setCategory(safety.getName());
        
        List<Post> posts = Collections.singletonList(validPost);
        Page<Post> postsPage = new PageImpl<>(posts, pageable, posts.size());

        when(postRepository.findPostsWithinRadius(any(Point.class), anyDouble(), eq(pageable)))
                .thenReturn(postsPage);

        // Create a subclass of PostServiceImpl that overrides mapPostToData
        PostServiceImpl customService = new PostServiceImpl(postRepository, categoryRepository, geometryFactory) {
            @Override
            protected Map<String, Object> mapPostToData(Post post) {
                Map<String, Object> result = new HashMap<>();
                // Put a non-Map object for the "post" key
                result.put("post", "This is a string, not a map");
                return result;
            }
        };

        // When
        Page<Map<String, Object>> result = customService.findPostsByLocation(
                centerLat, centerLon, radius, null, null, null, pageable);

        // Then
        assertNotNull(result);
        // The post should be filtered out because "post" is not a Map
        assertTrue(result.getContent().isEmpty());
        verify(postRepository).findPostsWithinRadius(any(Point.class), anyDouble(), eq(pageable));
    }
}