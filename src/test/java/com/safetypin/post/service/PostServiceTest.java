package com.safetypin.post.service;

import com.safetypin.post.exception.InvalidPostDataException;
import com.safetypin.post.exception.PostException;
import com.safetypin.post.exception.PostNotFoundException;
import com.safetypin.post.exception.UnauthorizedAccessException;
import com.safetypin.post.model.Category;
import com.safetypin.post.model.Post;
import com.safetypin.post.repository.CategoryRepository;
import com.safetypin.post.repository.PostRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.auth.InvalidCredentialsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
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

        private final Category safety = new Category("Safety"),
                        crime = new Category("Crime");
        @Mock
        private PostRepository postRepository;
        @Mock
        private CategoryRepository categoryRepository;
        @Mock
        private JwtService jwtService;
        private GeometryFactory geometryFactory;
        private PostServiceImpl postService;
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
                                Arguments.of("Valid title", "   ", "content", "Content is required"));
        }

        /**
         * Arguments provider for category validation tests
         */
        private static Stream<Arguments> categoryValidationProvider() {
                return Stream.of(
                                // categoryName, expectedMessage
                                Arguments.of(null, "Category is required"),
                                Arguments.of("", "Category is required"),
                                Arguments.of("   ", "Category is required"));
        }

        @BeforeEach
        void setup() {
                geometryFactory = new GeometryFactory();
                postService = new PostServiceImpl(postRepository, categoryRepository, jwtService);

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
        void testCreatePostTitleAndContentValidation(String title, String content, String fieldName,
                        String expectedMessage) {
                // Given
                Double latitude = 1.0;
                Double longitude = 2.0;
                Category category = safety;
                UUID userId = UUID.randomUUID();

                // When & Then
                Executable executable = () -> postService.createPost(title, content, latitude, longitude,
                                category.getName(), userId);
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
                UUID userId = UUID.randomUUID();

                // When & Then
                InvalidPostDataException exception = assertThrows(InvalidPostDataException.class, () -> postService
                                .createPost(title, content, latitude, longitude, categoryName, userId));
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
                UUID userId = UUID.randomUUID();

                // When & Then
                Executable executable = () -> postService.createPost(title, content, latitude, longitude,
                                category.getName(), userId);
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
                UUID userId = UUID.randomUUID();

                // When & Then
                Executable executable = () -> postService.createPost(title, content, latitude, longitude,
                                category.getName(), userId);
                InvalidPostDataException exception = assertThrows(InvalidPostDataException.class, executable);
                assertEquals("Location coordinates are required", exception.getMessage());
                verify(postRepository, never()).save(any(Post.class));
        }

        private final LocalDateTime now = LocalDateTime.now(),
                        yesterday = now.minusDays(1),
                        tomorrow = now.plusDays(1);

        @Test
        void testFindAllPaginated() {
                // Given
                Pageable pageable = PageRequest.of(0, 10);
                UUID userId = UUID.randomUUID(); // Changed from authorization header to UUID
                List<Post> posts = Arrays.asList(post1, post2, post3);
                Page<Post> expectedPage = new PageImpl<>(posts, pageable, posts.size());

                when(postRepository.findAll(pageable)).thenReturn(expectedPage);

                // When
                Page<Post> result = postService.findAllPaginated(userId, pageable);

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
                UUID userId = UUID.randomUUID();

                Post expectedPost = new Post();
                expectedPost.setTitle(title);
                expectedPost.setCaption(content);
                expectedPost.setLocation(geometryFactory.createPoint(new Coordinate(longitude, latitude)));
                expectedPost.setCategory(categoryName);

                when(categoryRepository.findByName(categoryName)).thenReturn(safetyCategory);
                when(postRepository.save(any(Post.class))).thenReturn(expectedPost);

                // When
                Post result = postService.createPost(title, content, latitude, longitude, categoryName, userId);

                // Then
                assertNotNull(result);
                assertEquals(title, result.getTitle());
                assertEquals(content, result.getCaption());
                assertEquals(categoryName, result.getCategory());

                verify(categoryRepository).findByName(categoryName);
                verify(postRepository).save(any(Post.class));
        }

        @Test
        void testCreatePost_NonExistentCategory() {
                // Given
                String title = "Test Post";
                String content = "This is a test post";
                Double latitude = 1.0;
                Double longitude = 2.0;
                String categoryName = "NonExistentCategory";
                UUID userId = UUID.randomUUID();

                when(categoryRepository.findByName(categoryName)).thenReturn(null);

                // When & Then
                InvalidPostDataException exception = assertThrows(InvalidPostDataException.class, () -> postService
                                .createPost(title, content, latitude, longitude, categoryName, userId));

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
                UUID userId = UUID.randomUUID();

                when(categoryRepository.findByName(categoryName)).thenReturn(safetyCategory);
                when(postRepository.save(any(Post.class))).thenThrow(new RuntimeException("Database error"));

                // When & Then
                PostException exception = assertThrows(PostException.class, () -> postService.createPost(title, content,
                                latitude, longitude, categoryName, userId));

                assertTrue(exception.getMessage().contains("Failed to save the post"));
                verify(categoryRepository).findByName(categoryName);
                verify(postRepository).save(any(Post.class));
        }

        @Test
        void testFindPostsByTimestampFeed_EmptyResult() throws InvalidCredentialsException {
                // Given
                Pageable pageable = PageRequest.of(0, 10);
                UUID userId = UUID.randomUUID(); // Changed from authorization header to UUID
                Page<Post> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);

                when(postRepository.findAll(pageable)).thenReturn(emptyPage);

                // When
                Page<Map<String, Object>> result = postService.findPostsByTimestampFeed(userId, pageable);

                // Then
                assertNotNull(result);
                assertTrue(result.getContent().isEmpty());
                assertEquals(0, result.getTotalElements());
                verify(postRepository).findAll(pageable);
        }

        // POSITIVE TEST CASES FOR SEARCH POSTS

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
                PostNotFoundException exception = assertThrows(PostNotFoundException.class,
                                () -> postService.findById(id));

                // Verify exception message contains the ID
                assertTrue(exception.getMessage().contains(id.toString()));
                verify(postRepository).findById(id);
        }

        @Test
        void testCreatePost_NullPostedBy() {
                // Given
                String title = "Test Post";
                String content = "This is a test post";
                Double latitude = 1.0;
                Double longitude = 2.0;
                String categoryName = "Safety";
                UUID postedBy = null; // Null user ID

                // When & Then
                InvalidPostDataException exception = assertThrows(InvalidPostDataException.class, () -> postService
                                .createPost(title, content, latitude, longitude, categoryName, postedBy));

                assertEquals("User ID (postedBy) is required", exception.getMessage());
                verify(postRepository, never()).save(any(Post.class));
        }

        @Test
        void testDeletePost_Success() {
                // Given
                UUID postId = UUID.randomUUID();
                UUID userId = UUID.randomUUID();
                Post post = new Post();
                post.setId(postId);
                post.setPostedBy(userId); // Same user ID (authorized)

                when(postRepository.findById(postId)).thenReturn(Optional.of(post));

                // When
                postService.deletePost(postId, userId);

                // Then
                verify(postRepository).findById(postId);
                verify(postRepository).delete(post);
        }

        @Test
        void testDeletePost_UnauthorizedAccess() {
                // Given
                UUID postId = UUID.randomUUID();
                UUID postOwnerId = UUID.randomUUID();
                UUID differentUserId = UUID.randomUUID(); // Different user trying to delete the post

                Post post = new Post();
                post.setId(postId);
                post.setPostedBy(postOwnerId); // Post belongs to a different user

                when(postRepository.findById(postId)).thenReturn(Optional.of(post));

                // When & Then
                UnauthorizedAccessException exception = assertThrows(UnauthorizedAccessException.class,
                                () -> postService.deletePost(postId, differentUserId));

                assertEquals("User not authorized to delete this post", exception.getMessage());
                verify(postRepository).findById(postId);
                verify(postRepository, never()).delete(any(Post.class));
        }

        @Test
        void testDeletePost_PostNotFound() {
                // Given
                UUID postId = UUID.randomUUID();
                UUID userId = UUID.randomUUID();

                when(postRepository.findById(postId)).thenReturn(Optional.empty());

                // When & Then
                PostNotFoundException exception = assertThrows(PostNotFoundException.class,
                                () -> postService.deletePost(postId, userId));

                assertTrue(exception.getMessage().contains(postId.toString()));
                verify(postRepository).findById(postId);
                verify(postRepository, never()).delete(any(Post.class));
        }

        @Test
        void testFindPostsByDistanceFeed_WithFilters_Success() throws InvalidCredentialsException {
                // Given
                Double userLat = 0.0;
                Double userLon = 0.0;
                List<String> categories = Collections.singletonList("Safety");
                String keyword = "test";
                LocalDateTime dateFrom = yesterday;
                LocalDateTime dateTo = tomorrow;
                Pageable pageable = PageRequest.of(0, 10);
                UUID userId = UUID.randomUUID(); // Changed from authorization header to UUID

                // Create sample posts
                Post matchingPost = new Post();
                matchingPost.setTitle("Test Post");
                matchingPost.setCaption("Test content");
                matchingPost.setLocation(geometryFactory.createPoint(new Coordinate(0.01, 0.01)));
                matchingPost.setLatitude(0.01);
                matchingPost.setLongitude(0.01);
                matchingPost.setCategory("Safety");
                matchingPost.setCreatedAt(now);

                List<Post> allPosts = Arrays.asList(matchingPost, post1, post2, post3);

                when(categoryRepository.findByName("Safety")).thenReturn(safetyCategory);
                when(postRepository.findAll()).thenReturn(allPosts);

                // When
                Page<Map<String, Object>> result = postService.findPostsByDistanceFeed(
                                userLat, userLon, categories, keyword, dateFrom, dateTo, userId, pageable);

                // Then
                assertNotNull(result);
                assertEquals(1, result.getContent().size()); // Only one post matches all criteria

                Map<String, Object> firstPostResult = result.getContent().getFirst();
                Map<String, Object> firstPostData = (Map<String, Object>) firstPostResult.get("post");
                assertEquals("Test Post", firstPostData.get("title"));
                assertEquals("Safety", firstPostData.get("category"));

                verify(categoryRepository).findByName("Safety");
                verify(postRepository).findAll();
        }

        @Test
        void testFindPostsByDistanceFeed_WithFilters_NoMatches() throws InvalidCredentialsException {
                // Given
                Double userLat = 0.0;
                Double userLon = 0.0;
                List<String> categories = Collections.singletonList("Safety");
                String keyword = "nonexistent"; // Keyword that won't match
                LocalDateTime dateFrom = yesterday;
                LocalDateTime dateTo = tomorrow;
                Pageable pageable = PageRequest.of(0, 10);
                UUID userId = UUID.randomUUID(); // Changed from authorization header to UUID

                when(categoryRepository.findByName("Safety")).thenReturn(safetyCategory);
                when(postRepository.findAll()).thenReturn(Arrays.asList(post1, post2, post3));

                // When
                Page<Map<String, Object>> result = postService.findPostsByDistanceFeed(
                                userLat, userLon, categories, keyword, dateFrom, dateTo, userId, pageable);

                // Then
                assertNotNull(result);
                assertTrue(result.getContent().isEmpty());

                verify(categoryRepository).findByName("Safety");
                verify(postRepository).findAll();
        }

        @Test
        void testFindPostsByDistanceFeed_WithFilters_NullTitle() throws InvalidCredentialsException {
                // Given
                Double userLat = 0.0;
                Double userLon = 0.0;
                List<String> categories = Collections.singletonList("Safety");
                String keyword = "test";
                LocalDateTime dateFrom = yesterday;
                LocalDateTime dateTo = tomorrow;
                Pageable pageable = PageRequest.of(0, 10);
                UUID userId = UUID.randomUUID(); // Changed from authorization header to UUID

                // Create sample post with null title but matching keyword in caption
                Post postWithNullTitle = new Post();
                postWithNullTitle.setTitle(null);
                postWithNullTitle.setCaption("This is a test caption");
                postWithNullTitle.setLocation(geometryFactory.createPoint(new Coordinate(0.01, 0.01)));
                postWithNullTitle.setLatitude(0.01);
                postWithNullTitle.setLongitude(0.01);
                postWithNullTitle.setCategory("Safety");
                postWithNullTitle.setCreatedAt(now);

                when(categoryRepository.findByName("Safety")).thenReturn(safetyCategory);
                when(postRepository.findAll()).thenReturn(Collections.singletonList(postWithNullTitle));

                // When
                Page<Map<String, Object>> result = postService.findPostsByDistanceFeed(
                                userLat, userLon, categories, keyword, dateFrom, dateTo, userId, pageable);

                // Then
                assertNotNull(result);
                assertEquals(1, result.getContent().size()); // Should match on caption

                verify(categoryRepository).findByName("Safety");
                verify(postRepository).findAll();
        }

        @Test
        void testFindPostsByDistanceFeed_WithFilters_NullCaption() throws InvalidCredentialsException {
                // Given
                Double userLat = 0.0;
                Double userLon = 0.0;
                List<String> categories = Collections.singletonList("Safety");
                String keyword = "test";
                LocalDateTime dateFrom = yesterday;
                LocalDateTime dateTo = tomorrow;
                Pageable pageable = PageRequest.of(0, 10);
                UUID userId = UUID.randomUUID(); // Changed from authorization header to UUID

                // Create sample post with null caption but matching keyword in title
                Post postWithNullCaption = new Post();
                postWithNullCaption.setTitle("This is a test title");
                postWithNullCaption.setCaption(null);
                postWithNullCaption.setLocation(geometryFactory.createPoint(new Coordinate(0.01, 0.01)));
                postWithNullCaption.setLatitude(0.01);
                postWithNullCaption.setLongitude(0.01);
                postWithNullCaption.setCategory("Safety");
                postWithNullCaption.setCreatedAt(now);

                when(categoryRepository.findByName("Safety")).thenReturn(safetyCategory);
                when(postRepository.findAll()).thenReturn(Collections.singletonList(postWithNullCaption));

                // When
                Page<Map<String, Object>> result = postService.findPostsByDistanceFeed(
                                userLat, userLon, categories, keyword, dateFrom, dateTo, userId, pageable);

                // Then
                assertNotNull(result);
                assertEquals(1, result.getContent().size()); // Should match on title

                verify(categoryRepository).findByName("Safety");
                verify(postRepository).findAll();
        }

        @Test
        void testFindPostsByDistanceFeed_WithFilters_NullCategory() throws InvalidCredentialsException {
                // Given
                Double userLat = 0.0;
                Double userLon = 0.0;
                List<String> categories = Collections.singletonList("Safety");
                String keyword = "test";
                LocalDateTime dateFrom = yesterday;
                LocalDateTime dateTo = tomorrow;
                Pageable pageable = PageRequest.of(0, 10);
                UUID userId = UUID.randomUUID(); // Changed from authorization header to UUID

                // Create sample post with null category
                Post postWithNullCategory = new Post();
                postWithNullCategory.setTitle("Test title");
                postWithNullCategory.setCaption("Test caption");
                postWithNullCategory.setLocation(geometryFactory.createPoint(new Coordinate(0.01, 0.01)));
                postWithNullCategory.setLatitude(0.01);
                postWithNullCategory.setLongitude(0.01);
                postWithNullCategory.setCategory(null);
                postWithNullCategory.setCreatedAt(now);

                when(categoryRepository.findByName("Safety")).thenReturn(safetyCategory);
                when(postRepository.findAll()).thenReturn(Collections.singletonList(postWithNullCategory));

                // When
                Page<Map<String, Object>> result = postService.findPostsByDistanceFeed(
                                userLat, userLon, categories, keyword, dateFrom, dateTo, userId, pageable);

                // Then
                assertNotNull(result);
                assertTrue(result.getContent().isEmpty()); // No match because of category filter

                verify(categoryRepository).findByName("Safety");
                verify(postRepository).findAll();
        }

        @Test
        void testFindPostsByDistanceFeed_WithFilters_DateOutOfRange() throws InvalidCredentialsException {
                // Given
                Double userLat = 0.0;
                Double userLon = 0.0;
                List<String> categories = Collections.singletonList("Safety");
                String keyword = "test";
                LocalDateTime dateFrom = yesterday;
                LocalDateTime dateTo = now; // Date range that excludes post3 (tomorrow)
                Pageable pageable = PageRequest.of(0, 10);
                UUID userId = UUID.randomUUID(); // Changed from authorization header to UUID

                // Create post inside the specified date range
                Post postInDateRange = new Post();
                postInDateRange.setTitle("Test Title");
                postInDateRange.setCaption("Test caption");
                postInDateRange.setLocation(geometryFactory.createPoint(new Coordinate(0.01, 0.01)));
                postInDateRange.setLatitude(0.01);
                postInDateRange.setLongitude(0.01);
                postInDateRange.setCategory("Safety");
                postInDateRange.setCreatedAt(now);

                // Create post outside the specified date range
                Post postOutsideDateRange = new Post();
                postOutsideDateRange.setTitle("Test Title");
                postOutsideDateRange.setCaption("Test caption");
                postOutsideDateRange.setLocation(geometryFactory.createPoint(new Coordinate(0.01, 0.01)));
                postOutsideDateRange.setLatitude(0.01);
                postOutsideDateRange.setLongitude(0.01);
                postOutsideDateRange.setCategory("Safety");
                postOutsideDateRange.setCreatedAt(tomorrow); // Outside date range

                when(categoryRepository.findByName("Safety")).thenReturn(safetyCategory);
                when(postRepository.findAll()).thenReturn(Arrays.asList(postInDateRange, postOutsideDateRange));

                // When
                Page<Map<String, Object>> result = postService.findPostsByDistanceFeed(
                                userLat, userLon, categories, keyword, dateFrom, dateTo, userId, pageable);

                // Then
                assertNotNull(result);
                assertEquals(1, result.getContent().size()); // Only one post in date range

                verify(categoryRepository).findByName("Safety");
                verify(postRepository).findAll();
        }

        @Test
        void testFindPostsByDistanceFeed_WithFilters_NonexistentCategoryThrowsException() {
                // Given
                Double userLat = 0.0;
                Double userLon = 0.0;
                List<String> categories = Collections.singletonList("NonexistentCategory");
                String keyword = "test";
                LocalDateTime dateFrom = yesterday;
                LocalDateTime dateTo = tomorrow;
                Pageable pageable = PageRequest.of(0, 10);
                UUID userId = UUID.randomUUID(); // Changed from authorization header to UUID

                when(categoryRepository.findByName("NonexistentCategory")).thenReturn(null);

                // When & Then
                InvalidPostDataException exception = assertThrows(InvalidPostDataException.class, () -> postService
                                .findPostsByDistanceFeed(
                                                userLat, userLon, categories, keyword, dateFrom, dateTo,
                                                userId, pageable));

                assertEquals("Category does not exist: NonexistentCategory", exception.getMessage());
                verify(categoryRepository).findByName("NonexistentCategory");
        }

        @Test
        void testFindPostsByTimestampFeed_WithFilters_Success() throws InvalidCredentialsException {
                // Given
                List<String> categories = Collections.singletonList("Safety");
                String keyword = "test";
                LocalDateTime dateFrom = yesterday;
                LocalDateTime dateTo = tomorrow;
                Pageable pageable = PageRequest.of(0, 10);
                UUID userId = UUID.randomUUID(); // Changed from authorization header to UUID

                // Create sample post matching all filters
                Post matchingPost = new Post();
                matchingPost.setTitle("Test Post");
                matchingPost.setCaption("Test content");
                matchingPost.setCategory("Safety");
                matchingPost.setCreatedAt(now);

                List<Post> allPosts = Arrays.asList(matchingPost, post1, post2, post3);

                when(categoryRepository.findByName("Safety")).thenReturn(safetyCategory);
                when(postRepository.findAll()).thenReturn(allPosts);

                // When
                Page<Map<String, Object>> result = postService.findPostsByTimestampFeed(
                                categories, keyword, dateFrom, dateTo, userId, pageable);

                // Then
                assertNotNull(result);
                assertEquals(1, result.getContent().size()); // Only matching post

                Map<String, Object> firstPostResult = result.getContent().getFirst();
                Map<String, Object> firstPostData = (Map<String, Object>) firstPostResult.get("post");
                assertEquals("Test Post", firstPostData.get("title"));
                assertEquals("Safety", firstPostData.get("category"));

                verify(categoryRepository).findByName("Safety");
                verify(postRepository).findAll();
        }

        @Test
        void testFindPostsByTimestampFeed_WithFilters_NoMatches() throws InvalidCredentialsException {
                // Given
                List<String> categories = Collections.singletonList("Safety");
                String keyword = "nonexistent"; // No posts will match this
                LocalDateTime dateFrom = yesterday;
                LocalDateTime dateTo = tomorrow;
                Pageable pageable = PageRequest.of(0, 10);
                UUID userId = UUID.randomUUID(); // Changed from authorization header to UUID

                when(categoryRepository.findByName("Safety")).thenReturn(safetyCategory);
                when(postRepository.findAll()).thenReturn(Arrays.asList(post1, post2, post3));

                // When
                Page<Map<String, Object>> result = postService.findPostsByTimestampFeed(
                                categories, keyword, dateFrom, dateTo, userId, pageable);

                // Then
                assertNotNull(result);
                assertTrue(result.getContent().isEmpty());

                verify(categoryRepository).findByName("Safety");
                verify(postRepository).findAll();
        }

        @Test
        void testFindPostsByTimestampFeed_WithFilters_DateFiltering() throws InvalidCredentialsException {
                // Given
                List<String> categories = null; // No category filter
                String keyword = null; // No keyword filter
                LocalDateTime dateFrom = yesterday;
                LocalDateTime dateTo = now; // Exclude post3 (tomorrow)
                Pageable pageable = PageRequest.of(0, 10);
                UUID userId = UUID.randomUUID(); // Changed from authorization header to UUID

                when(postRepository.findAll()).thenReturn(Arrays.asList(post1, post2, post3));

                // When
                Page<Map<String, Object>> result = postService.findPostsByTimestampFeed(
                                categories, keyword, dateFrom, dateTo, userId, pageable);

                // Then
                assertNotNull(result);
                assertEquals(2, result.getContent().size()); // post1 and post2 (excluding post3)

                verify(postRepository).findAll();
        }

        @Test
        void testFindPostsByTimestampFeed_WithFilters_Pagination() throws InvalidCredentialsException {
                // Given
                List<String> categories = null;
                String keyword = null;
                LocalDateTime dateFrom = null;
                LocalDateTime dateTo = null;
                int pageSize = 2;
                Pageable pageable = PageRequest.of(0, pageSize);
                UUID userId = UUID.randomUUID(); // Changed from authorization header to UUID

                // Create 5 posts to test pagination
                List<Post> allPosts = new ArrayList<>();
                for (int i = 0; i < 5; i++) {
                        Post post = new Post();
                        post.setTitle("Post " + i);
                        post.setCaption("Content " + i);
                        post.setCategory("Safety");
                        post.setCreatedAt(now.plusHours(i)); // Different timestamps
                        allPosts.add(post);
                }

                when(postRepository.findAll()).thenReturn(allPosts);

                // When - First page
                Page<Map<String, Object>> firstPageResult = postService.findPostsByTimestampFeed(
                                categories, keyword, dateFrom, dateTo, userId, pageable);

                // Then - First page
                assertNotNull(firstPageResult);
                assertEquals(2, firstPageResult.getContent().size());
                assertEquals(5, firstPageResult.getTotalElements());
                assertEquals(3, firstPageResult.getTotalPages());
                assertTrue(firstPageResult.hasNext());
                assertFalse(firstPageResult.hasPrevious());

                // When - Second page
                Pageable secondPageable = PageRequest.of(1, pageSize);
                Page<Map<String, Object>> secondPageResult = postService.findPostsByTimestampFeed(
                                categories, keyword, dateFrom, dateTo, userId, secondPageable);

                // Then - Second page
                assertEquals(2, secondPageResult.getContent().size());
                assertEquals(5, secondPageResult.getTotalElements());
                assertEquals(1, secondPageResult.getNumber());
                assertTrue(secondPageResult.hasNext());
                assertTrue(secondPageResult.hasPrevious());

                verify(postRepository, times(2)).findAll();
        }

        @Test
        void testFindPostsByTimestampFeed_WithFilters_NonexistentCategoryThrowsException() {
                // Given
                List<String> categories = Collections.singletonList("NonexistentCategory");
                String keyword = "test";
                LocalDateTime dateFrom = yesterday;
                LocalDateTime dateTo = tomorrow;
                Pageable pageable = PageRequest.of(0, 10);
                UUID userId = UUID.randomUUID(); // Changed from authorization header to UUID

                when(categoryRepository.findByName("NonexistentCategory")).thenReturn(null);

                // When & Then
                InvalidPostDataException exception = assertThrows(InvalidPostDataException.class, () -> postService
                                .findPostsByTimestampFeed(
                                                categories, keyword, dateFrom, dateTo, userId, pageable));

                assertEquals("Category does not exist: NonexistentCategory", exception.getMessage());
                verify(categoryRepository).findByName("NonexistentCategory");
        }

        @Test
        void testFindPostsByTimestampFeed_WithFilters_SortingOrder() throws InvalidCredentialsException {
                // Given
                List<String> categories = null;
                String keyword = null;
                LocalDateTime dateFrom = null;
                LocalDateTime dateTo = null;
                Pageable pageable = PageRequest.of(0, 10);
                UUID userId = UUID.randomUUID(); // Changed from authorization header to UUID

                // Post with earliest date
                Post earliestPost = new Post();
                earliestPost.setTitle("Earliest Post");
                earliestPost.setCreatedAt(yesterday);

                // Post with middle date
                Post middlePost = new Post();
                middlePost.setTitle("Middle Post");
                middlePost.setCreatedAt(now);

                // Post with latest date
                Post latestPost = new Post();
                latestPost.setTitle("Latest Post");
                latestPost.setCreatedAt(tomorrow);

                List<Post> allPosts = Arrays.asList(middlePost, latestPost, earliestPost); // Unsorted order

                when(postRepository.findAll()).thenReturn(allPosts);

                // When
                Page<Map<String, Object>> result = postService.findPostsByTimestampFeed(
                                categories, keyword, dateFrom, dateTo, userId, pageable);

                // Then
                assertNotNull(result);
                assertEquals(3, result.getContent().size());

                // Verify sorting order (earliest first)
                List<Map<String, Object>> content = result.getContent();
                Map<String, Object> firstPostMap = content.get(0);
                Map<String, Object> secondPostMap = content.get(1);
                Map<String, Object> thirdPostMap = content.get(2);

                Map<String, Object> firstPostData = (Map<String, Object>) firstPostMap.get("post");
                Map<String, Object> secondPostData = (Map<String, Object>) secondPostMap.get("post");
                Map<String, Object> thirdPostData = (Map<String, Object>) thirdPostMap.get("post");

                assertEquals("Earliest Post", firstPostData.get("title"));
                assertEquals("Middle Post", secondPostData.get("title"));
                assertEquals("Latest Post", thirdPostData.get("title"));

                verify(postRepository).findAll();
        }

        @Test
        void testFindPostsByTimestampFeed_WithFilters_NullTitleAndCaption() throws InvalidCredentialsException {
                // Given
                List<String> categories = null;
                String keyword = "test"; // Search for "test"
                LocalDateTime dateFrom = null;
                LocalDateTime dateTo = null;
                Pageable pageable = PageRequest.of(0, 10);
                UUID userId = UUID.randomUUID(); // Changed from authorization header to UUID

                // Post with null title but matching caption
                Post postWithNullTitle = new Post();
                postWithNullTitle.setTitle(null);
                postWithNullTitle.setCaption("This is a test caption");
                postWithNullTitle.setCreatedAt(now);

                // Post with null caption but matching title
                Post postWithNullCaption = new Post();
                postWithNullCaption.setTitle("This is a test title");
                postWithNullCaption.setCaption(null);
                postWithNullCaption.setCreatedAt(now);

                // Post with both null (won't match)
                Post postWithBothNull = new Post();
                postWithBothNull.setTitle(null);
                postWithBothNull.setCaption(null);
                postWithBothNull.setCreatedAt(now);

                List<Post> allPosts = Arrays.asList(postWithNullTitle, postWithNullCaption, postWithBothNull);

                when(postRepository.findAll()).thenReturn(allPosts);

                // When
                Page<Map<String, Object>> result = postService.findPostsByTimestampFeed(
                                categories, keyword, dateFrom, dateTo, userId, pageable);

                // Then
                assertNotNull(result);
                assertEquals(2, result.getContent().size()); // Two posts match the keyword

                verify(postRepository).findAll();
        }

        // Test for null ID in mapPostToData
        @Test
        void testMapPostToDataWithNullId() {
                // Given
                UUID userId = UUID.randomUUID();

                Post post = new Post();
                post.setId(null); // Null ID
                post.setTitle("Test Title");
                post.setCaption("Test Caption");
                post.setLocation(geometryFactory.createPoint(new Coordinate(0.1, 0.1)));
                post.setCategory("Safety");

                // When
                Map<String, Object> result = postService.mapPostToData(post, userId);

                // Then
                assertNotNull(result);
                assertNull(result.get("id"));
        }

        // Test findPostsByDistanceFeed with filters with out-of-range dates
        @Test
        void testFindPostsByDistanceFeed_WithFilters_OutOfRangeDates() throws InvalidCredentialsException {
                // Given
                Double userLat = 0.0;
                Double userLon = 0.0;
                List<String> categories = null;
                String keyword = null;
                LocalDateTime dateFrom = tomorrow.plusDays(1); // Future date
                LocalDateTime dateTo = tomorrow.plusDays(2); // Future date
                Pageable pageable = PageRequest.of(0, 10);
                UUID userId = UUID.randomUUID(); // Changed from authorization header to UUID

                when(postRepository.findAll()).thenReturn(Arrays.asList(post1, post2, post3));

                // When
                Page<Map<String, Object>> result = postService.findPostsByDistanceFeed(
                                userLat, userLon, categories, keyword, dateFrom, dateTo, userId, pageable);

                // Then
                assertNotNull(result);
                assertTrue(result.getContent().isEmpty()); // No posts should match the future dates
                verify(postRepository).findAll();
        }

        // Test for findPostsByTimestampFeed with complex filters
        @Test
        void testFindPostsByTimestampFeed_WithComplexFilters() throws InvalidCredentialsException {
                // Given
                List<String> categories = Arrays.asList("Safety");
                String keyword = "test";
                LocalDateTime dateFrom = yesterday.minusDays(1);
                LocalDateTime dateTo = tomorrow.plusDays(1);
                Pageable pageable = PageRequest.of(0, 10);
                UUID userId = UUID.randomUUID(); // Changed from authorization header to UUID

                // Create posts with varied data to test filters
                Post matchingPost1 = new Post();
                matchingPost1.setTitle("Test 1");
                matchingPost1.setCaption("Test caption 1");
                matchingPost1.setCategory("Safety");
                matchingPost1.setCreatedAt(yesterday);

                Post matchingPost2 = new Post();
                matchingPost2.setTitle("Another test");
                matchingPost2.setCaption("Another caption with test");
                matchingPost2.setCategory("Safety");
                matchingPost2.setCreatedAt(tomorrow);

                Post nonMatchingPost = new Post();
                nonMatchingPost.setTitle("No match");
                nonMatchingPost.setCaption("No match in caption");
                nonMatchingPost.setCategory("Crime");
                nonMatchingPost.setCreatedAt(now);

                when(categoryRepository.findByName("Safety")).thenReturn(safetyCategory);
                when(postRepository.findAll()).thenReturn(Arrays.asList(matchingPost1, matchingPost2, nonMatchingPost));

                // When
                Page<Map<String, Object>> result = postService.findPostsByTimestampFeed(
                                categories, keyword, dateFrom, dateTo, userId, pageable);

                // Then
                assertNotNull(result);
                assertEquals(2, result.getContent().size()); // Should match the 2 matching posts

                // Verify the posts are sorted by createdAt (earliest first)
                Map<String, Object> firstPostData = (Map<String, Object>) result.getContent().get(0).get("post");
                Map<String, Object> secondPostData = (Map<String, Object>) result.getContent().get(1).get("post");

                assertEquals("Test 1", firstPostData.get("title"));
                assertEquals("Another test", secondPostData.get("title"));

                verify(categoryRepository).findByName("Safety");
                verify(postRepository).findAll();
        }

        // Test for pagination edge case - page number beyond available pages but valid
        @Test
        void testFindPostsByTimestampFeed_WithValidButEmptyPage() throws InvalidCredentialsException {
                // Given
                Pageable pageable = PageRequest.of(10, 5); // Page 10, which doesn't exist
                UUID userId = UUID.randomUUID(); // Changed from authorization header to UUID

                // Empty list but total elements is 2
                when(postRepository.findAll(pageable)).thenReturn(new PageImpl<>(Collections.emptyList(), pageable, 2));

                // When
                Page<Map<String, Object>> result = postService.findPostsByTimestampFeed(userId, pageable);

                // Then
                assertNotNull(result);
                assertTrue(result.isEmpty()); // Page is empty
                assertEquals(2, result.getTotalElements()); // Total elements is still 2
                assertEquals(1, result.getTotalPages()); // Total pages is 1
                assertTrue(result.hasPrevious()); // Has previous page
                assertFalse(result.hasNext()); // No next page

                verify(postRepository).findAll(pageable);
        }

        // Test creating post with exact max values for validation
        @Test
        void testCreatePost_WithMaxValues() {
                // Given
                String title = "Test Post";
                String content = "This is a test post";
                Double latitude = 90.0; // Max valid latitude
                Double longitude = 180.0; // Max valid longitude
                String categoryName = "Safety";
                UUID userId = UUID.randomUUID();

                Post expectedPost = new Post();
                expectedPost.setTitle(title);
                expectedPost.setCaption(content);
                expectedPost.setLocation(geometryFactory.createPoint(new Coordinate(longitude, latitude)));
                expectedPost.setCategory(categoryName);

                when(categoryRepository.findByName(categoryName)).thenReturn(safetyCategory);
                when(postRepository.save(any(Post.class))).thenReturn(expectedPost);

                // When
                Post result = postService.createPost(title, content, latitude, longitude, categoryName, userId);

                // Then
                assertNotNull(result);
                assertEquals(title, result.getTitle());
                assertEquals(content, result.getCaption());
                assertEquals(categoryName, result.getCategory());
                assertEquals(latitude, result.getLatitude());
                assertEquals(longitude, result.getLongitude());

                verify(categoryRepository).findByName(categoryName);
                verify(postRepository).save(any(Post.class));
        }

        // Test exception in repository during createPost
        @Test
        void testCreatePost_RepositoryThrowsException() {
                // Given
                String title = "Test Post";
                String content = "This is a test post";
                Double latitude = 1.0;
                Double longitude = 2.0;
                String categoryName = "Safety";
                UUID userId = UUID.randomUUID();

                when(categoryRepository.findByName(categoryName)).thenReturn(safetyCategory);
                when(postRepository.save(any(Post.class)))
                                .thenThrow(new RuntimeException("Database connection error"));

                // When & Then
                PostException exception = assertThrows(PostException.class,
                                () -> postService.createPost(title, content, latitude, longitude, categoryName,
                                                userId));

                assertTrue(exception.getMessage().contains("Failed to save the post"));
                verify(categoryRepository).findByName(categoryName);
                verify(postRepository).save(any(Post.class));
        }

        // Add a test for findPostsByTimestampFeed with filters and null user ID
        @Test
        void testFindPostsByTimestampFeed_WithFilters_NullUserId() {
                // Given
                List<String> categories = Collections.singletonList("Safety");
                String keyword = "test";
                LocalDateTime dateFrom = yesterday;
                LocalDateTime dateTo = tomorrow;
                Pageable pageable = PageRequest.of(0, 10);
                UUID userId = null; // Null user ID

                // When & Then
                InvalidCredentialsException exception = assertThrows(InvalidCredentialsException.class,
                                () -> postService.findPostsByTimestampFeed(
                                                categories, keyword, dateFrom, dateTo, userId, pageable));

                assertEquals("User ID is required", exception.getMessage());
                verifyNoInteractions(postRepository);
        }

        @Test
        void testFindPostsByTimestampFeed_SortingByTimestamp() throws InvalidCredentialsException {
                // Given
                Pageable pageable = PageRequest.of(0, 10);
                UUID userId = UUID.randomUUID(); // Changed from authorization header to UUID

                // Create posts with different timestamps in unsorted order
                Post post1 = new Post();
                post1.setTitle("Post 1");
                post1.setCreatedAt(now.plusHours(2)); // Latest

                Post post2 = new Post();
                post2.setTitle("Post 2");
                post2.setCreatedAt(now); // Middle

                Post post3 = new Post();
                post3.setTitle("Post 3");
                post3.setCreatedAt(now.minusHours(2)); // Earliest

                List<Post> unsortedPosts = Arrays.asList(post1, post2, post3); // Unsorted by time
                Page<Post> postsPage = new PageImpl<>(unsortedPosts, pageable, unsortedPosts.size());

                when(postRepository.findAll(pageable)).thenReturn(postsPage);

                // When
                Page<Map<String, Object>> result = postService.findPostsByTimestampFeed(userId, pageable);

                // Then
                assertNotNull(result);
                assertEquals(3, result.getContent().size());

                // Verify posts are sorted by timestamp (earliest first)
                List<Map<String, Object>> content = result.getContent();
                Map<String, Object> firstPost = content.get(0);
                Map<String, Object> secondPost = content.get(1);
                Map<String, Object> thirdPost = content.get(2);

                Map<String, Object> firstPostData = (Map<String, Object>) firstPost.get("post");
                Map<String, Object> secondPostData = (Map<String, Object>) secondPost.get("post");
                Map<String, Object> thirdPostData = (Map<String, Object>) thirdPost.get("post");

                assertEquals("Post 3", firstPostData.get("title")); // Earliest post should be first
                assertEquals("Post 2", secondPostData.get("title")); // Middle post should be second
                assertEquals("Post 1", thirdPostData.get("title")); // Latest post should be last

                verify(postRepository).findAll(pageable);
        }

        @Test
        void testFindPostsByTimestampFeed_PaginationParameters() throws InvalidCredentialsException {
                // Given
                int pageSize = 2;
                int pageNumber = 1; // Second page
                Pageable pageable = PageRequest.of(pageNumber, pageSize);
                UUID userId = UUID.randomUUID(); // Changed from authorization header to UUID

                // Create more posts than the page size
                List<Post> allPosts = new ArrayList<>();
                for (int i = 0; i < 5; i++) {
                        Post post = new Post();
                        post.setTitle("Post " + i);
                        post.setCreatedAt(now.plusHours(i));
                        allPosts.add(post);
                }

                // Create a page with only posts for the second page
                List<Post> secondPagePosts = allPosts.subList(2, 4); // Posts 2 and 3
                Page<Post> postsPage = new PageImpl<>(secondPagePosts, pageable, allPosts.size());

                when(postRepository.findAll(pageable)).thenReturn(postsPage);

                // When
                Page<Map<String, Object>> result = postService.findPostsByTimestampFeed(userId, pageable);

                // Then
                assertNotNull(result);
                assertEquals(2, result.getContent().size());
                assertEquals(5, result.getTotalElements());
                assertEquals(3, result.getTotalPages());
                assertEquals(1, result.getNumber()); // Page number (0-based)
                assertTrue(result.hasNext());
                assertTrue(result.hasPrevious());

                verify(postRepository).findAll(pageable);
        }

        // Update test for null user ID
        @Test
        void testFindPostsByTimestampFeed_WithNullUserId() {
                // Given
                Pageable pageable = PageRequest.of(0, 10);
                UUID userId = null; // Null user ID

                // When & Then
                InvalidCredentialsException exception = assertThrows(InvalidCredentialsException.class,
                                () -> postService.findPostsByTimestampFeed(userId, pageable));

                assertEquals("User ID is required", exception.getMessage());
                verifyNoInteractions(postRepository);
        }

        // Add test for null user ID in distance feed
        @Test
        void testFindPostsByDistanceFeed_WithNullUserId() {
                // Given
                Double userLat = 0.0;
                Double userLon = 0.0;
                List<String> categories = null;
                String keyword = null;
                LocalDateTime dateFrom = null;
                LocalDateTime dateTo = null;
                Pageable pageable = PageRequest.of(0, 10);
                UUID userId = null; // Null user ID

                // When & Then
                InvalidCredentialsException exception = assertThrows(InvalidCredentialsException.class,
                                () -> postService.findPostsByDistanceFeed(
                                                userLat, userLon, categories, keyword, dateFrom, dateTo, userId,
                                                pageable));

                assertEquals("User ID is required", exception.getMessage());
                verifyNoInteractions(postRepository);
        }
}