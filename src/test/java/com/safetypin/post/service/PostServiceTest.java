package com.safetypin.post.service;

import com.safetypin.post.dto.LocationFilter;
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
                postService = new PostServiceImpl(postRepository, categoryRepository, geometryFactory, jwtService);

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

        // Test findPostsByLocation with null repository response
        @Test
        void testFindPostsByLocationWithNullResponse() throws InvalidCredentialsException {
                // Given
                double centerLat = 0.15;
                double centerLon = 0.15;
                double radius = 20.0; // 20 km
                Pageable pageable = PageRequest.of(0, 10);
                String authorizationHeader = "Bearer test-token";
                LocationFilter filter = new LocationFilter(null, null, null);

                when(postRepository.findPostsWithinRadius(any(Point.class), anyDouble(), eq(pageable)))
                        .thenReturn(null);

                // When
                Page<Map<String, Object>> result = postService.findPostsByLocation(
                        centerLat, centerLon, radius, filter, authorizationHeader, pageable);

                // Then
                assertNotNull(result);
                assertTrue(result.getContent().isEmpty());
        }

        // Test findPostsByLocation with category filter
        @Test
        void testFindPostsByLocationWithCategoryFilter() throws InvalidCredentialsException {
                // Given
                double centerLat = 0.15;
                double centerLon = 0.15;
                double radius = 20.0; // 20 km
                String category = "Crime";
                LocationFilter filter = new LocationFilter(category, null, null);
                Pageable pageable = PageRequest.of(0, 10);
                String authorizationHeader = "Bearer test-token";

                List<Post> posts = Arrays.asList(post1, post2, post3);
                Page<Post> postsPage = new PageImpl<>(posts, pageable, posts.size());

                when(postRepository.findPostsWithinRadius(any(Point.class), anyDouble(), eq(pageable)))
                        .thenReturn(postsPage);

                // When
                Page<Map<String, Object>> result = postService.findPostsByLocation(
                        centerLat, centerLon, radius, filter, authorizationHeader, pageable);

                // Then
                assertNotNull(result);
                // Only post2 has Crime category
                assertEquals(1, result.getContent().size());
        }

        // Test findPostsByLocation with distance filtering
        @Test
        void testFindPostsByLocationWithDistanceFiltering() throws InvalidCredentialsException {
                // Given
                double centerLat = 0.1; // Same as post1's latitude
                double centerLon = 0.1; // Same as post1's longitude
                double radius = 5.0; // Small radius to filter out post2 and post3
                Pageable pageable = PageRequest.of(0, 10);
                String authorizationHeader = "Bearer test-token";
                LocationFilter filter = new LocationFilter(null, null, null);

                List<Post> posts = Arrays.asList(post1, post2, post3);
                Page<Post> postsPage = new PageImpl<>(posts, pageable, posts.size());

                when(postRepository.findPostsWithinRadius(any(Point.class), anyDouble(), eq(pageable)))
                        .thenReturn(postsPage);

                // When
                Page<Map<String, Object>> result = postService.findPostsByLocation(
                        centerLat, centerLon, radius, filter, authorizationHeader, pageable);

                // Then
                assertNotNull(result);
                // Only post1 should be within the small radius
                assertEquals(1, result.getContent().size());

                Map<String, Object> postResult = result.getContent().getFirst();
                Map<String, Object> postData = (Map<String, Object>) postResult.get("post");
                assertEquals(0.0, postResult.get("distance"));
                assertEquals(post1.getCategory(), postData.get("category"));
        }

        private final LocalDateTime now = LocalDateTime.now(),
                yesterday = now.minusDays(1),
                tomorrow = now.plusDays(1);

        @Test
        void testFindAllPaginated() {
                // Given
                Pageable pageable = PageRequest.of(0, 10);
                String authorizationHeader = "Bearer test-token";
                List<Post> posts = Arrays.asList(post1, post2, post3);
                Page<Post> expectedPage = new PageImpl<>(posts, pageable, posts.size());

                when(postRepository.findAll(pageable)).thenReturn(expectedPage);

                // When
                Page<Post> result = postService.findAllPaginated(authorizationHeader, pageable);

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
        void testFindPostsByLocationWithDateFilters() throws InvalidCredentialsException {
                // Given
                double centerLat = 0.15;
                double centerLon = 0.15;
                double radius = 20.0; // 20 km
                LocalDateTime dateFrom = yesterday;
                LocalDateTime dateTo = tomorrow;
                LocationFilter filter = new LocationFilter(null, dateFrom, dateTo);
                Pageable pageable = PageRequest.of(0, 10);
                String authorizationHeader = "Bearer test-token";

                List<Post> posts = Arrays.asList(post1, post2);
                Page<Post> postsPage = new PageImpl<>(posts, pageable, posts.size());

                when(postRepository.findPostsByDateRange(any(Point.class), anyDouble(), eq(dateFrom), eq(dateTo),
                        eq(pageable)))
                        .thenReturn(postsPage);

                // When
                Page<Map<String, Object>> result = postService.findPostsByLocation(
                        centerLat, centerLon, radius, filter, authorizationHeader, pageable);

                // Then
                assertNotNull(result);
                assertEquals(2, result.getContent().size());
                verify(postRepository).findPostsByDateRange(any(Point.class), anyDouble(), eq(dateFrom), eq(dateTo),
                        eq(pageable));
        }

        @Test
        void testFindPostsByLocationWithPostWithoutLocation() throws InvalidCredentialsException {
                // Given
                double centerLat = 0.15;
                double centerLon = 0.15;
                double radius = 20.0; // 20 km
                Pageable pageable = PageRequest.of(0, 10);
                String authorizationHeader = "Bearer test-token";
                LocationFilter filter = new LocationFilter(null, null, null);

                List<Post> posts = Collections.singletonList(postWithoutLocation);
                Page<Post> postsPage = new PageImpl<>(posts, pageable, posts.size());

                when(postRepository.findPostsWithinRadius(any(Point.class), anyDouble(), eq(pageable)))
                        .thenReturn(postsPage);

                // When
                Page<Map<String, Object>> result = postService.findPostsByLocation(
                        centerLat, centerLon, radius, filter, authorizationHeader, pageable);

                // Then
                assertNotNull(result);
                // Post without location should be filtered out because distance check would
                // fail
                assertEquals(0, result.getContent().size());
        }

        @Test
        void testFindPostsByLocationWithDateFiltersNull() throws InvalidCredentialsException {
                // Given
                double centerLat = 0.15;
                double centerLon = 0.15;
                double radius = 20.0; // 20 km
                Pageable pageable = PageRequest.of(0, 10);
                String authorizationHeader = "Bearer test-token";
                LocationFilter filter = new LocationFilter(null, null, null);

                List<Post> posts = Arrays.asList(post1, post2, post3);
                Page<Post> postsPage = new PageImpl<>(posts, pageable, posts.size());

                when(postRepository.findPostsWithinRadius(any(Point.class), anyDouble(), eq(pageable)))
                        .thenReturn(postsPage);

                // When
                Page<Map<String, Object>> result = postService.findPostsByLocation(
                        centerLat, centerLon, radius, filter, authorizationHeader, pageable);

                // Then
                assertNotNull(result);
                assertEquals(2, result.getContent().size());
                verify(postRepository).findPostsWithinRadius(any(Point.class), anyDouble(), eq(pageable));
        }

        @Test
        void testFindPostsByTimestampFeed_EmptyResult() throws InvalidCredentialsException {
                // Given
                Pageable pageable = PageRequest.of(0, 10);
                String authorizationHeader = "Bearer test-token";
                Page<Post> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);

                when(postRepository.findAll(pageable)).thenReturn(emptyPage);

                // When
                Page<Map<String, Object>> result = postService.findPostsByTimestampFeed(authorizationHeader, pageable);

                // Then
                assertNotNull(result);
                assertTrue(result.getContent().isEmpty());
                assertEquals(0, result.getTotalElements());
                verify(postRepository).findAll(pageable);
        }

        @Test
        void testSearchPosts_WithKeywordOnly() throws InvalidCredentialsException {
                // Given
                Double centerLat = 0.15;
                Double centerLon = 0.15;
                Double radius = 20.0; // 20 km
                String keyword = "test";
                List<String> categories = null;
                Pageable pageable = PageRequest.of(0, 10);
                String authorizationHeader = "Bearer test-token";
                UUID userId = UUID.randomUUID();

                when(jwtService.getUserIdFromAuthorizationHeader(authorizationHeader)).thenReturn(userId);

                List<Post> posts = Arrays.asList(post1, post2);
                Page<Post> postsPage = new PageImpl<>(posts, pageable, posts.size());

                when(postRepository.searchPostsByKeyword(any(Point.class), anyDouble(), eq(keyword), eq(pageable)))
                        .thenReturn(postsPage);

                // When
                Page<Map<String, Object>> result = postService.searchPosts(
                        centerLat, centerLon, radius, keyword, categories, authorizationHeader, pageable);

                // Then
                assertNotNull(result);
                assertEquals(2, result.getContent().size());
                verify(postRepository).searchPostsByKeyword(any(Point.class), anyDouble(), eq(keyword), eq(pageable));
                verify(jwtService).getUserIdFromAuthorizationHeader(authorizationHeader);
        }

        @Test
        void testSearchPosts_WithCategoriesOnly() throws InvalidCredentialsException {
                // Given
                Double centerLat = 0.15;
                Double centerLon = 0.15;
                Double radius = 20.0; // 20 km
                String keyword = null;
                List<String> categories = Arrays.asList("Safety", "Crime");
                Pageable pageable = PageRequest.of(0, 10);
                String authorizationHeader = "Bearer test-token";
                UUID userId = UUID.randomUUID();

                when(jwtService.getUserIdFromAuthorizationHeader(authorizationHeader)).thenReturn(userId);

                List<Post> posts = Arrays.asList(post1, post3);
                Page<Post> postsPage = new PageImpl<>(posts, pageable, posts.size());

                when(categoryRepository.findByName("Safety")).thenReturn(safetyCategory);
                when(categoryRepository.findByName("Crime")).thenReturn(crime);
                when(postRepository.searchPostsByCategories(any(Point.class), anyDouble(), eq(categories),
                        eq(pageable)))
                        .thenReturn(postsPage);

                // When
                Page<Map<String, Object>> result = postService.searchPosts(
                        centerLat, centerLon, radius, keyword, categories, authorizationHeader, pageable);

                // Then
                assertNotNull(result);
                assertEquals(1, result.getContent().size());
                verify(postRepository).searchPostsByCategories(any(Point.class), anyDouble(), eq(categories),
                        eq(pageable));
                verify(categoryRepository, times(2)).findByName(anyString());
                verify(jwtService).getUserIdFromAuthorizationHeader(authorizationHeader);
        }

        // Test category UUID handling in findPostsByLocation

        @Test
        void testSearchPosts_WithKeywordAndCategories() throws InvalidCredentialsException {
                // Given
                Double centerLat = 0.15;
                Double centerLon = 0.15;
                Double radius = 20.0; // 20 km
                String keyword = "test";
                List<String> categories = List.of("Safety");
                Pageable pageable = PageRequest.of(0, 10);
                String authorizationHeader = "Bearer test-token";
                UUID userId = UUID.randomUUID();

                when(jwtService.getUserIdFromAuthorizationHeader(authorizationHeader)).thenReturn(userId);

                List<Post> posts = Collections.singletonList(post1);
                Page<Post> postsPage = new PageImpl<>(posts, pageable, posts.size());

                when(categoryRepository.findByName("Safety")).thenReturn(safetyCategory);
                when(postRepository.searchPostsByKeywordAndCategories(any(Point.class), anyDouble(), eq(keyword),
                        eq(categories), eq(pageable)))
                        .thenReturn(postsPage);

                // When
                Page<Map<String, Object>> result = postService.searchPosts(
                        centerLat, centerLon, radius, keyword, categories, authorizationHeader, pageable);

                // Then
                assertNotNull(result);
                assertEquals(1, result.getContent().size());
                verify(postRepository).searchPostsByKeywordAndCategories(any(Point.class), anyDouble(), eq(keyword),
                        eq(categories), eq(pageable));
                verify(categoryRepository).findByName("Safety");
                verify(jwtService).getUserIdFromAuthorizationHeader(authorizationHeader);
        }

        @Test
        void testSearchPosts_NoSearchCriteria() {
                // Given
                Double centerLat = 0.15;
                Double centerLon = 0.15;
                Double radius = 20.0; // 20 km
                String keyword = ""; // Empty keyword
                List<String> categories = Collections.emptyList(); // Empty categories
                Pageable pageable = PageRequest.of(0, 10);
                String authorizationHeader = "Bearer test-token";

                // When
                Page<Map<String, Object>> result = postService.searchPosts(
                        centerLat, centerLon, radius, keyword, categories, authorizationHeader, pageable);

                // Then
                assertNotNull(result);
                assertTrue(result.getContent().isEmpty());
                assertEquals(0, result.getTotalElements());

                // Verify no repository methods were called
                verifyNoInteractions(postRepository);
                verifyNoInteractions(jwtService);
        }

        @Test
        void testSearchPosts_NullResultFromRepository() throws InvalidCredentialsException {
                // Given
                Double centerLat = 0.15;
                Double centerLon = 0.15;
                Double radius = 20.0; // 20 km
                String keyword = "test";
                List<String> categories = null;
                Pageable pageable = PageRequest.of(0, 10);
                String authorizationHeader = "Bearer test-token";
                UUID userId = UUID.randomUUID();

                when(jwtService.getUserIdFromAuthorizationHeader(authorizationHeader)).thenReturn(userId);
                when(postRepository.searchPostsByKeyword(any(Point.class), anyDouble(), eq(keyword), eq(pageable)))
                        .thenReturn(null);

                // When
                Page<Map<String, Object>> result = postService.searchPosts(
                        centerLat, centerLon, radius, keyword, categories, authorizationHeader, pageable);

                // Then
                assertNotNull(result);
                assertTrue(result.getContent().isEmpty());
                assertEquals(0, result.getTotalElements());
                verify(jwtService).getUserIdFromAuthorizationHeader(authorizationHeader);
        }

        @Test
        void testSearchPosts_DistanceFiltering() throws InvalidCredentialsException {
                // Given
                Double centerLat = 0.1;
                Double centerLon = 0.1;
                Double radius = 5.0; // Small radius to filter out distant posts
                String keyword = "test";
                List<String> categories = null;
                Pageable pageable = PageRequest.of(0, 10);
                String authorizationHeader = "Bearer test-token";
                UUID userId = UUID.randomUUID();

                when(jwtService.getUserIdFromAuthorizationHeader(authorizationHeader)).thenReturn(userId);

                // post1 is close, post2 and post3 are farther away
                List<Post> posts = Arrays.asList(post1, post2, post3);
                Page<Post> postsPage = new PageImpl<>(posts, pageable, posts.size());

                when(postRepository.searchPostsByKeyword(any(Point.class), anyDouble(), eq(keyword), eq(pageable)))
                        .thenReturn(postsPage);

                // When
                Page<Map<String, Object>> result = postService.searchPosts(
                        centerLat, centerLon, radius, keyword, categories, authorizationHeader, pageable);

                // Then
                assertNotNull(result);
                // Only post1 should be within the small radius
                assertEquals(1, result.getContent().size());

                Map<String, Object> postResult = result.getContent().getFirst();
                Map<String, Object> postData = (Map<String, Object>) postResult.get("post");
                assertEquals(post1.getCategory(), postData.get("category"));
                verify(jwtService).getUserIdFromAuthorizationHeader(authorizationHeader);
        }

        @Test
        void testSearchPosts_Pagination() throws InvalidCredentialsException {
                // Given
                Double centerLat = 0.15;
                Double centerLon = 0.15;
                Double radius = 20.0; // 20 km
                String keyword = "test";
                List<String> categories = null;
                int pageSize = 2;
                Pageable pageable = PageRequest.of(0, pageSize);
                String authorizationHeader = "Bearer test-token";
                UUID userId = UUID.randomUUID();

                when(jwtService.getUserIdFromAuthorizationHeader(authorizationHeader)).thenReturn(userId);

                // Create posts to fill multiple pages
                List<Post> posts = Arrays.asList(post1, post2);
                Page<Post> postsPage = new PageImpl<>(posts, pageable, 5); // 5 total elements but only 2 in this page

                when(postRepository.searchPostsByKeyword(any(Point.class), anyDouble(), eq(keyword), eq(pageable)))
                        .thenReturn(postsPage);

                // When
                Page<Map<String, Object>> result = postService.searchPosts(
                        centerLat, centerLon, radius, keyword, categories, authorizationHeader, pageable);

                // Then
                assertNotNull(result);
                assertEquals(2, result.getContent().size());
                assertEquals(2, result.getTotalElements()); // This will be the size of the filtered results
                assertEquals(1, result.getTotalPages());
                verify(jwtService).getUserIdFromAuthorizationHeader(authorizationHeader);
        }

        @Test
        void testSearchPosts_NonExistentCategory() {
                // Given
                Double centerLat = 0.15;
                Double centerLon = 0.15;
                Double radius = 20.0; // 20 km
                String keyword = null;
                List<String> categories = Arrays.asList("Safety", "NonExistentCategory");
                Pageable pageable = PageRequest.of(0, 10);
                String authorizationHeader = "Bearer test-token";

                when(categoryRepository.findByName("Safety")).thenReturn(safetyCategory);
                when(categoryRepository.findByName("NonExistentCategory")).thenReturn(null);

                // When & Then
                InvalidPostDataException exception = assertThrows(InvalidPostDataException.class,
                        () -> postService.searchPosts(centerLat, centerLon, radius, keyword, categories,
                                authorizationHeader, pageable));

                assertEquals("Category does not exist: NonExistentCategory", exception.getMessage());
                verify(categoryRepository, times(2)).findByName(anyString());
                verify(postRepository, never()).searchPostsByCategories(any(), anyDouble(), any(), any());
                verifyNoInteractions(jwtService);
        }

        @Test
        void testSearchPosts_RepositoryException() throws InvalidCredentialsException {
                // Given
                Double centerLat = 0.15;
                Double centerLon = 0.15;
                Double radius = 20.0; // 20 km
                String keyword = "test";
                List<String> categories = null;
                Pageable pageable = PageRequest.of(0, 10);
                String authorizationHeader = "Bearer test-token";
                UUID userId = UUID.randomUUID();

                when(jwtService.getUserIdFromAuthorizationHeader(authorizationHeader)).thenReturn(userId);
                when(postRepository.searchPostsByKeyword(any(Point.class), anyDouble(), eq(keyword), eq(pageable)))
                        .thenThrow(new RuntimeException("Database error"));

                // When & Then
                RuntimeException exception = assertThrows(RuntimeException.class,
                        () -> postService.searchPosts(centerLat, centerLon, radius, keyword, categories,
                                authorizationHeader, pageable));

                assertEquals("Database error", exception.getMessage());
                verify(jwtService).getUserIdFromAuthorizationHeader(authorizationHeader);
        }

        @Test
        void testSearchPosts_EmptyCategoriesList() throws InvalidCredentialsException {
                // Given
                Double centerLat = 0.15;
                Double centerLon = 0.15;
                Double radius = 20.0; // 20 km
                String keyword = "test";
                List<String> categories = Collections.emptyList();
                Pageable pageable = PageRequest.of(0, 10);
                String authorizationHeader = "Bearer test-token";
                UUID userId = UUID.randomUUID();

                when(jwtService.getUserIdFromAuthorizationHeader(authorizationHeader)).thenReturn(userId);

                List<Post> posts = Arrays.asList(post1, post2);
                Page<Post> postsPage = new PageImpl<>(posts, pageable, posts.size());

                when(postRepository.searchPostsByKeyword(any(Point.class), anyDouble(), eq(keyword), eq(pageable)))
                        .thenReturn(postsPage);

                // When
                Page<Map<String, Object>> result = postService.searchPosts(
                        centerLat, centerLon, radius, keyword, categories, authorizationHeader, pageable);

                // Then
                assertNotNull(result);
                assertEquals(2, result.getContent().size());
                verify(postRepository).searchPostsByKeyword(any(Point.class), anyDouble(), eq(keyword), eq(pageable));
                // No category validation should happen
                verifyNoInteractions(categoryRepository);
                verify(jwtService).getUserIdFromAuthorizationHeader(authorizationHeader);
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
                String authorizationHeader = "Bearer test-token";

                // When
                Page<Map<String, Object>> result = postService.searchPosts(
                        centerLat, centerLon, radius, keyword, categories, authorizationHeader, pageable);

                // Then
                assertNotNull(result);
                assertTrue(result.getContent().isEmpty());
                // Should be treated as no search criteria provided
                verifyNoInteractions(postRepository);
                verifyNoInteractions(jwtService);
        }

        // Also need to update the PostServiceImpl test constructor method that's used
        // in one test
        @Test
        void testFindPostsByLocation_PostObjectNotMap() throws InvalidCredentialsException {
                // Given
                double centerLat = 0.15;
                double centerLon = 0.15;
                double radius = 20.0; // 20 km
                Pageable pageable = PageRequest.of(0, 10);
                String authorizationHeader = "Bearer test-token";
                LocationFilter filter = new LocationFilter(null, null, null);
                UUID userId = UUID.randomUUID();

                when(jwtService.getUserIdFromAuthorizationHeader(authorizationHeader)).thenReturn(userId);

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
                PostServiceImpl customService = new PostServiceImpl(postRepository, categoryRepository, geometryFactory,
                        jwtService) {
                        @Override
                        protected Map<String, Object> mapPostToData(Post post, UUID userId) {
                                Map<String, Object> result = new HashMap<>();
                                // Put a non-Map object for the "post" key
                                result.put("post", "This is a string, not a map");
                                return result;
                        }
                };

                // When
                Page<Map<String, Object>> result = customService.findPostsByLocation(
                        centerLat, centerLon, radius, filter, authorizationHeader, pageable);

                // Then
                assertNotNull(result);
                // The post should be filtered out because "post" is not a Map
                assertTrue(result.getContent().isEmpty());
                verify(postRepository).findPostsWithinRadius(any(Point.class), anyDouble(), eq(pageable));
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
        void testFindPostsByLocationWithNullLocation() throws InvalidCredentialsException {
                // Given
                double centerLat = 0.15;
                double centerLon = 0.15;
                double radius = 20.0; // 20 km
                Pageable pageable = PageRequest.of(0, 10);
                LocationFilter filter = new LocationFilter(null, null, null);

                // Create a post with null location for this test
                Post postWithNullLocation = new Post();
                postWithNullLocation.setLocation(null); // Force null location
                postWithNullLocation.setCategory(safety.getName());
                postWithNullLocation.setCreatedAt(now);

                List<Post> posts = Collections.singletonList(postWithNullLocation);
                Page<Post> postsPage = new PageImpl<>(posts, pageable, posts.size());
                String authorizationHeader = "Bearer test-token";

                when(postRepository.findPostsWithinRadius(any(Point.class), anyDouble(), eq(pageable)))
                        .thenReturn(postsPage);

                // When
                Page<Map<String, Object>> result = postService.findPostsByLocation(
                        centerLat, centerLon, radius, filter, authorizationHeader, pageable);

                // Then
                assertNotNull(result);
                // The post should be filtered out during distance calculation/filtering
                assertTrue(result.getContent().isEmpty());

                // Since null location results in distance = 0.0, verify post processing
                verify(postRepository).findPostsWithinRadius(any(Point.class), anyDouble(), eq(pageable));
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
        void testFindPostsByLocation_NullValueInDistance() throws InvalidCredentialsException {
                // Given
                double centerLat = 0.15;
                double centerLon = 0.15;
                double radius = 20.0; // 20 km
                Pageable pageable = PageRequest.of(0, 10);
                String authorizationHeader = "Bearer test-token";
                LocationFilter filter = new LocationFilter(null, null, null);
                UUID userId = UUID.randomUUID();

                when(jwtService.getUserIdFromAuthorizationHeader(authorizationHeader)).thenReturn(userId);

                // Create a post with location that has valid coordinates
                // but we'll make our custom service handle it specially
                Post postWithLocation = new Post();
                postWithLocation.setLocation(geometryFactory.createPoint(new Coordinate(0.1, 0.1)));
                postWithLocation.setLatitude(0.1);
                postWithLocation.setLongitude(0.1);
                postWithLocation.setCategory(safety.getName());

                List<Post> posts = Collections.singletonList(postWithLocation);
                Page<Post> postsPage = new PageImpl<>(posts, pageable, posts.size());

                when(postRepository.findPostsWithinRadius(any(Point.class), anyDouble(), eq(pageable)))
                        .thenReturn(postsPage);

                // Create a custom implementation that will return null for distance calculation
                PostServiceImpl customService = new PostServiceImpl(postRepository, categoryRepository, geometryFactory,
                        jwtService) {
                        @Override
                        protected Map<String, Object> mapPostToData(Post post, UUID userId) {
                                Map<String, Object> postData = new HashMap<>();
                                postData.put("id", post.getId());
                                postData.put("title", post.getTitle());
                                postData.put("caption", post.getCaption());
                                // Explicitly set latitude and longitude to null which will cause the filter to
                                // exclude this post
                                postData.put("latitude", null);
                                postData.put("longitude", null);
                                postData.put("createdAt", post.getCreatedAt());
                                postData.put("category", post.getCategory());
                                postData.put("upvoteCount", post.getUpvoteCount());
                                postData.put("downvoteCount", post.getDownvoteCount());
                                postData.put("currentVote", post.currentVote(userId));
                                postData.put("postedBy", post.getPostedBy());
                                return postData;
                        }
                };

                // When
                Page<Map<String, Object>> result = customService.findPostsByLocation(
                        centerLat, centerLon, radius, filter, authorizationHeader, pageable);

                // Then
                assertNotNull(result);
                assertTrue(result.getContent().isEmpty()); // Should be filtered out
                verify(postRepository).findPostsWithinRadius(any(Point.class), anyDouble(), eq(pageable));
        }

        @Test
        void testFindPostsByLocation_DistanceGreaterThanRadius() throws InvalidCredentialsException {
                // Given
                double centerLat = 0.0;
                double centerLon = 0.0;
                double radius = 0.1; // Very small radius in km
                Pageable pageable = PageRequest.of(0, 10);
                String authorizationHeader = "Bearer test-token";
                LocationFilter filter = new LocationFilter(null, null, null);
                UUID userId = UUID.randomUUID();

                when(jwtService.getUserIdFromAuthorizationHeader(authorizationHeader)).thenReturn(userId);

                // Create a post that will be outside the radius
                Post distantPost = new Post();
                distantPost.setLocation(geometryFactory.createPoint(new Coordinate(1.0, 1.0))); // Far away
                distantPost.setLatitude(1.0);
                distantPost.setLongitude(1.0);
                distantPost.setCategory(safety.getName());

                List<Post> posts = Collections.singletonList(distantPost);
                Page<Post> postsPage = new PageImpl<>(posts, pageable, posts.size());

                when(postRepository.findPostsWithinRadius(any(Point.class), anyDouble(), eq(pageable)))
                        .thenReturn(postsPage);

                // When
                Page<Map<String, Object>> result = postService.findPostsByLocation(
                        centerLat, centerLon, radius, filter, authorizationHeader, pageable);

                // Then
                assertNotNull(result);
                assertTrue(result.getContent().isEmpty()); // Should be filtered out as distance > radius
                verify(postRepository).findPostsWithinRadius(any(Point.class), anyDouble(), eq(pageable));
        }

        @Test
        void testFindPostsByLocation_NullLatitudeInPostMap() throws InvalidCredentialsException {
                // Given
                double centerLat = 0.15;
                double centerLon = 0.15;
                double radius = 20.0; // 20 km
                Pageable pageable = PageRequest.of(0, 10);
                String authorizationHeader = "Bearer test-token";
                LocationFilter filter = new LocationFilter(null, null, null);
                UUID userId = UUID.randomUUID();

                when(jwtService.getUserIdFromAuthorizationHeader(authorizationHeader)).thenReturn(userId);

                // Create a custom implementation to return a post with null latitude in the map
                PostServiceImpl customService = new PostServiceImpl(postRepository, categoryRepository, geometryFactory,
                        jwtService) {
                        @Override
                        protected Map<String, Object> mapPostToData(Post post, UUID userId) {
                                Map<String, Object> postData = new HashMap<>();
                                postData.put("id", post.getId());
                                postData.put("title", post.getTitle());
                                postData.put("caption", post.getCaption());
                                postData.put("latitude", null); // Explicitly set latitude to null
                                postData.put("longitude", post.getLongitude());
                                postData.put("createdAt", post.getCreatedAt());
                                postData.put("category", post.getCategory());
                                postData.put("upvoteCount", post.getUpvoteCount());
                                postData.put("downvoteCount", post.getDownvoteCount());
                                postData.put("currentVote", post.currentVote(userId));
                                postData.put("postedBy", post.getPostedBy());
                                return postData;
                        }
                };

                // Create a post with valid location
                Post post = new Post();
                post.setLocation(geometryFactory.createPoint(new Coordinate(0.1, 0.1)));
                post.setLatitude(0.1);
                post.setLongitude(0.1);
                post.setCategory(safety.getName());

                List<Post> posts = Collections.singletonList(post);
                Page<Post> postsPage = new PageImpl<>(posts, pageable, posts.size());

                when(postRepository.findPostsWithinRadius(any(Point.class), anyDouble(), eq(pageable)))
                        .thenReturn(postsPage);

                // When
                Page<Map<String, Object>> result = customService.findPostsByLocation(
                        centerLat, centerLon, radius, filter, authorizationHeader, pageable);

                // Then
                assertNotNull(result);
                assertTrue(result.getContent().isEmpty()); // Should be filtered out due to null latitude
                verify(postRepository).findPostsWithinRadius(any(Point.class), anyDouble(), eq(pageable));
        }

        @Test
        void testFindPostsByLocation_NullLongitudeInPostMap() throws InvalidCredentialsException {
                // Given
                double centerLat = 0.15;
                double centerLon = 0.15;
                double radius = 20.0; // 20 km
                Pageable pageable = PageRequest.of(0, 10);
                String authorizationHeader = "Bearer test-token";
                LocationFilter filter = new LocationFilter(null, null, null);
                UUID userId = UUID.randomUUID();

                when(jwtService.getUserIdFromAuthorizationHeader(authorizationHeader)).thenReturn(userId);

                // Create a custom implementation to return a post with null longitude in the
                // map
                PostServiceImpl customService = new PostServiceImpl(postRepository, categoryRepository, geometryFactory,
                        jwtService) {
                        @Override
                        protected Map<String, Object> mapPostToData(Post post, UUID userId) {
                                Map<String, Object> postData = new HashMap<>();
                                postData.put("id", post.getId());
                                postData.put("title", post.getTitle());
                                postData.put("caption", post.getCaption());
                                postData.put("latitude", post.getLatitude());
                                postData.put("longitude", null); // Explicitly set longitude to null
                                postData.put("createdAt", post.getCreatedAt());
                                postData.put("category", post.getCategory());
                                postData.put("upvoteCount", post.getUpvoteCount());
                                postData.put("downvoteCount", post.getDownvoteCount());
                                postData.put("currentVote", post.currentVote(userId));
                                postData.put("postedBy", post.getPostedBy());
                                return postData;
                        }
                };

                // Create a post with valid location
                Post post = new Post();
                post.setLocation(geometryFactory.createPoint(new Coordinate(0.1, 0.1)));
                post.setLatitude(0.1);
                post.setLongitude(0.1);
                post.setCategory(safety.getName());

                List<Post> posts = Collections.singletonList(post);
                Page<Post> postsPage = new PageImpl<>(posts, pageable, posts.size());

                when(postRepository.findPostsWithinRadius(any(Point.class), anyDouble(), eq(pageable)))
                        .thenReturn(postsPage);

                // When
                Page<Map<String, Object>> result = customService.findPostsByLocation(
                        centerLat, centerLon, radius, filter, authorizationHeader, pageable);

                // Then
                assertNotNull(result);
                assertTrue(result.getContent().isEmpty()); // Should be filtered out due to null longitude
                verify(postRepository).findPostsWithinRadius(any(Point.class), anyDouble(), eq(pageable));
        }

        @Test
        void testFindPostsByLocation_CategoryFilterWithNoCategorySpecified() throws InvalidCredentialsException {
                // Given
                double centerLat = 0.15;
                double centerLon = 0.15;
                double radius = 20.0; // 20 km
                Pageable pageable = PageRequest.of(0, 10);
                String authorizationHeader = "Bearer test-token";
                LocationFilter filter = new LocationFilter(null, null, null);
                UUID userId = UUID.randomUUID();

                when(jwtService.getUserIdFromAuthorizationHeader(authorizationHeader)).thenReturn(userId);

                // Mix of posts with different categories
                List<Post> posts = Arrays.asList(post1, post2, post3);
                Page<Post> postsPage = new PageImpl<>(posts, pageable, posts.size());

                when(postRepository.findPostsWithinRadius(any(Point.class), anyDouble(), eq(pageable)))
                        .thenReturn(postsPage);

                // When
                Page<Map<String, Object>> result = postService.findPostsByLocation(
                        centerLat, centerLon, radius, filter, authorizationHeader, pageable);

                // Then
                assertNotNull(result);
                // All posts should be included since we're not filtering by category
                assertEquals(2, result.getContent().size()); // This includes post1 and post2 as they're within range
                verify(postRepository).findPostsWithinRadius(any(Point.class), anyDouble(), eq(pageable));
        }

        @Test
        void testFindPostsByLocation_NullDateButNonNullCategory() throws InvalidCredentialsException {
                // Given
                double centerLat = 0.15;
                double centerLon = 0.15;
                double radius = 20.0; // 20 km
                String category = "Safety"; // Non-null category
                Pageable pageable = PageRequest.of(0, 10);
                String authorizationHeader = "Bearer test-token";
                LocationFilter filter = new LocationFilter(category, null, null);
                UUID userId = UUID.randomUUID();

                when(jwtService.getUserIdFromAuthorizationHeader(authorizationHeader)).thenReturn(userId);

                List<Post> posts = Arrays.asList(post1, post3); // Both have Safety category
                Page<Post> postsPage = new PageImpl<>(posts, pageable, posts.size());

                when(postRepository.findPostsWithinRadius(any(Point.class), anyDouble(), eq(pageable)))
                        .thenReturn(postsPage);

                // When
                Page<Map<String, Object>> result = postService.findPostsByLocation(
                        centerLat, centerLon, radius, filter, authorizationHeader, pageable);

                // Then
                assertNotNull(result);
                assertEquals(1, result.getContent().size()); // Only post1 should be in range and match category

                Map<String, Object> postMap = result.getContent().getFirst();
                Map<String, Object> postData = (Map<String, Object>) postMap.get("post");
                assertEquals("Safety", postData.get("category"));
                verify(postRepository).findPostsWithinRadius(any(Point.class), anyDouble(), eq(pageable));
        }

        @Test
        void testFindPostsByLocation_WithNullPostsInList() throws InvalidCredentialsException {
                // Given
                double centerLat = 0.15;
                double centerLon = 0.15;
                double radius = 20.0; // 20 km
                Pageable pageable = PageRequest.of(0, 10);
                String authorizationHeader = "Bearer test-token";
                LocationFilter filter = new LocationFilter(null, null, null);
                UUID userId = UUID.randomUUID();

                when(jwtService.getUserIdFromAuthorizationHeader(authorizationHeader)).thenReturn(userId);

                // Create a list with one valid post and one null post
                List<Post> posts = new ArrayList<>();
                posts.add(post1);
                posts.add(null); // Add a null post to test null handling
                Page<Post> postsPage = new PageImpl<>(posts, pageable, posts.size());

                when(postRepository.findPostsWithinRadius(any(Point.class), anyDouble(), eq(pageable)))
                        .thenReturn(postsPage);

                // When
                Page<Map<String, Object>> result = postService.findPostsByLocation(
                        centerLat, centerLon, radius, filter, authorizationHeader, pageable);

                // Then
                assertNotNull(result);
                assertEquals(1, result.getContent().size()); // Only post1 should be processed
                verify(postRepository).findPostsWithinRadius(any(Point.class), anyDouble(), eq(pageable));
        }

        @Test
        void testFindPostsByLocation_NullDistance() throws InvalidCredentialsException {
                // Given
                double centerLat = 0.15;
                double centerLon = 0.15;
                double radius = 20.0; // 20 km
                Pageable pageable = PageRequest.of(0, 10);
                String authorizationHeader = "Bearer test-token";
                LocationFilter filter = new LocationFilter(null, null, null);
                UUID userId = UUID.randomUUID();

                when(jwtService.getUserIdFromAuthorizationHeader(authorizationHeader)).thenReturn(userId);

                // Create a post with valid location
                Post post = new Post();
                post.setLocation(geometryFactory.createPoint(new Coordinate(0.1, 0.1)));
                post.setLatitude(0.1);
                post.setLongitude(0.1);
                post.setCategory("Safety");

                List<Post> posts = Collections.singletonList(post);
                Page<Post> postsPage = new PageImpl<>(posts, pageable, posts.size());

                when(postRepository.findPostsWithinRadius(any(Point.class), anyDouble(), eq(pageable)))
                        .thenReturn(postsPage);

                // When & Then - can't directly test the lambda filter that uses
                // entry.getValue().getKey()
                // So we'll verify the method works with our custom service
                Page<Map<String, Object>> result = postService.findPostsByLocation(
                        centerLat, centerLon, radius, filter, authorizationHeader, pageable);

                assertNotNull(result);
                assertEquals(1, result.getContent().size()); // Should find the valid post
        }

        @Test
        void testFindPostsByLocation_DateFromOnlyNotNull() throws InvalidCredentialsException {
                // Given
                double centerLat = 0.15;
                double centerLon = 0.15;
                double radius = 20.0; // 20 km
                LocalDateTime dateFrom = yesterday;
                Pageable pageable = PageRequest.of(0, 10);
                String authorizationHeader = "Bearer test-token";
                LocationFilter filter = new LocationFilter(null, dateFrom, null);

                List<Post> posts = Arrays.asList(post1, post2);
                Page<Post> postsPage = new PageImpl<>(posts, pageable, posts.size());

                // Since only one date is provided, it should use findPostsWithinRadius
                when(postRepository.findPostsWithinRadius(any(Point.class), anyDouble(), eq(pageable)))
                        .thenReturn(postsPage);

                // When
                Page<Map<String, Object>> result = postService.findPostsByLocation(
                        centerLat, centerLon, radius, filter, authorizationHeader, pageable);

                // Then
                assertNotNull(result);
                assertEquals(2, result.getContent().size());
                verify(postRepository).findPostsWithinRadius(any(Point.class), anyDouble(), eq(pageable));
                // Verify it did NOT call findPostsByDateRange
                verify(postRepository, never()).findPostsByDateRange(any(Point.class), anyDouble(), any(), any(),
                        any());
        }

        @Test
        void testFindPostsByLocation_DateToOnlyNotNull() throws InvalidCredentialsException {
                // Given
                double centerLat = 0.15;
                double centerLon = 0.15;
                double radius = 20.0; // 20 km
                LocalDateTime dateTo = tomorrow;
                Pageable pageable = PageRequest.of(0, 10);
                String authorizationHeader = "Bearer test-token";
                LocationFilter filter = new LocationFilter(null, null, dateTo);

                List<Post> posts = Arrays.asList(post1, post2);
                Page<Post> postsPage = new PageImpl<>(posts, pageable, posts.size());

                // Since only one date is provided, it should use findPostsWithinRadius
                when(postRepository.findPostsWithinRadius(any(Point.class), anyDouble(), eq(pageable)))
                        .thenReturn(postsPage);

                // When
                Page<Map<String, Object>> result = postService.findPostsByLocation(
                        centerLat, centerLon, radius, filter, authorizationHeader, pageable);

                // Then
                assertNotNull(result);
                assertEquals(2, result.getContent().size());
                verify(postRepository).findPostsWithinRadius(any(Point.class), anyDouble(), eq(pageable));
                // Verify it did NOT call findPostsByDateRange
                verify(postRepository, never()).findPostsByDateRange(any(Point.class), anyDouble(), any(), any(),
                        any());
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
        void testSearchPosts_AuthenticationErrorFromInvalidCredentials() throws InvalidCredentialsException {
                // Given
                Double centerLat = 0.15;
                Double centerLon = 0.15;
                Double radius = 20.0;
                String keyword = "test";
                List<String> categories = null;
                Pageable pageable = PageRequest.of(0, 10);
                String authorizationHeader = "Bearer invalid-token";

                when(jwtService.getUserIdFromAuthorizationHeader(authorizationHeader))
                        .thenThrow(new InvalidCredentialsException("Invalid credentials"));

                // When & Then
                PostException exception = assertThrows(PostException.class,
                        () -> postService.searchPosts(centerLat, centerLon, radius, keyword, categories,
                                authorizationHeader, pageable));

                assertTrue(exception.getMessage().contains("Authentication error while searching posts"));
                verify(jwtService).getUserIdFromAuthorizationHeader(authorizationHeader);
                verifyNoInteractions(postRepository);
        }

        @Test
        void testSearchPosts_NullCategory() {
                // Given
                Double centerLat = 0.15;
                Double centerLon = 0.15;
                Double radius = 20.0;
                String keyword = "test";
                List<String> categories = Arrays.asList("Safety", null); // One category is null
                Pageable pageable = PageRequest.of(0, 10);
                String authorizationHeader = "Bearer test-token";

                when(categoryRepository.findByName("Safety")).thenReturn(safetyCategory);

                // When & Then
                InvalidPostDataException exception = assertThrows(InvalidPostDataException.class,
                        () -> postService.searchPosts(centerLat, centerLon, radius, keyword, categories,
                                authorizationHeader, pageable));

                assertEquals("Category does not exist: null", exception.getMessage());
                verify(categoryRepository).findByName("Safety");
                verifyNoInteractions(postRepository);
                verifyNoInteractions(jwtService);
        }

        @Test
        void testSearchPosts_InvalidKeywordWithValidCategories() throws InvalidCredentialsException {
                // Given
                Double centerLat = 0.15;
                Double centerLon = 0.15;
                Double radius = 20.0;
                String keyword = "   "; // Just whitespace - should be treated as empty
                List<String> categories = Collections.singletonList("Safety");
                Pageable pageable = PageRequest.of(0, 10);
                String authorizationHeader = "Bearer test-token";
                UUID userId = UUID.randomUUID();

                when(jwtService.getUserIdFromAuthorizationHeader(authorizationHeader)).thenReturn(userId);
                when(categoryRepository.findByName("Safety")).thenReturn(safetyCategory);

                List<Post> posts = Arrays.asList(post1, post3);
                Page<Post> postsPage = new PageImpl<>(posts, pageable, posts.size());

                when(postRepository.searchPostsByCategories(any(Point.class), anyDouble(), eq(categories),
                        eq(pageable))).thenReturn(postsPage);

                // When
                Page<Map<String, Object>> result = postService.searchPosts(
                        centerLat, centerLon, radius, keyword, categories, authorizationHeader, pageable);

                // Then
                assertNotNull(result);
                assertEquals(1, result.getContent().size());
                verify(categoryRepository).findByName("Safety");
                verify(postRepository).searchPostsByCategories(any(Point.class), anyDouble(), eq(categories),
                        eq(pageable));
                verify(jwtService).getUserIdFromAuthorizationHeader(authorizationHeader);
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
        void testValidateCategories_AllCategoriesExist() throws InvalidCredentialsException {
                // Given
                Double centerLat = 0.15;
                Double centerLon = 0.15;
                Double radius = 20.0;
                String keyword = "test";
                List<String> categories = Arrays.asList("Safety", "Crime");
                Pageable pageable = PageRequest.of(0, 10);
                String authorizationHeader = "Bearer test-token";
                UUID userId = UUID.randomUUID();

                when(jwtService.getUserIdFromAuthorizationHeader(authorizationHeader)).thenReturn(userId);
                when(categoryRepository.findByName("Safety")).thenReturn(safetyCategory);
                when(categoryRepository.findByName("Crime")).thenReturn(crime);

                List<Post> posts = Arrays.asList(post1, post2);
                Page<Post> postsPage = new PageImpl<>(posts, pageable, posts.size());

                when(postRepository.searchPostsByKeywordAndCategories(any(Point.class), anyDouble(), eq(keyword),
                        eq(categories), eq(pageable))).thenReturn(postsPage);

                // When
                Page<Map<String, Object>> result = postService.searchPosts(
                        centerLat, centerLon, radius, keyword, categories, authorizationHeader, pageable);

                // Then
                assertNotNull(result);
                assertEquals(2, result.getContent().size());
                verify(categoryRepository).findByName("Safety");
                verify(categoryRepository).findByName("Crime");
                verify(postRepository).searchPostsByKeywordAndCategories(any(Point.class), anyDouble(), eq(keyword),
                        eq(categories), eq(pageable));
        }

        @Test
        void testValidateCategories_EmptyCategory() {
                // Given
                Double centerLat = 0.15;
                Double centerLon = 0.15;
                Double radius = 20.0;
                String keyword = "test";
                List<String> categories = Arrays.asList("Safety", ""); // One empty category
                Pageable pageable = PageRequest.of(0, 10);
                String authorizationHeader = "Bearer test-token";

                when(categoryRepository.findByName("Safety")).thenReturn(safetyCategory);
                when(categoryRepository.findByName("")).thenReturn(null);

                // When & Then
                InvalidPostDataException exception = assertThrows(InvalidPostDataException.class,
                        () -> postService.searchPosts(centerLat, centerLon, radius, keyword, categories,
                                authorizationHeader, pageable));

                assertEquals("Category does not exist: ", exception.getMessage());
                verify(categoryRepository).findByName("Safety");
                verify(categoryRepository).findByName("");
                verifyNoInteractions(postRepository);
                verifyNoInteractions(jwtService);
        }

        @Test
        void testValidateCategories_NonExistentCategoryAmongMany() {
                // Given
                Double centerLat = 0.15;
                Double centerLon = 0.15;
                Double radius = 20.0;
                String keyword = "test";
                // Multiple valid categories but one non-existent
                List<String> categories = Arrays.asList("Safety", "Crime", "NonExistentCategory");
                Pageable pageable = PageRequest.of(0, 10);
                String authorizationHeader = "Bearer test-token";

                when(categoryRepository.findByName("Safety")).thenReturn(safetyCategory);
                when(categoryRepository.findByName("Crime")).thenReturn(crime);
                when(categoryRepository.findByName("NonExistentCategory")).thenReturn(null);

                // When & Then
                InvalidPostDataException exception = assertThrows(InvalidPostDataException.class,
                        () -> postService.searchPosts(centerLat, centerLon, radius, keyword, categories,
                                authorizationHeader, pageable));

                assertEquals("Category does not exist: NonExistentCategory", exception.getMessage());
                verify(categoryRepository).findByName("Safety");
                verify(categoryRepository).findByName("Crime");
                verify(categoryRepository).findByName("NonExistentCategory");
                verifyNoInteractions(postRepository);
                verifyNoInteractions(jwtService);
        }

        @Test
        void testValidateCategories_NullCategory() {
                // Given
                Double centerLat = 0.15;
                Double centerLon = 0.15;
                Double radius = 20.0;
                String keyword = "test";
                // Include a null category in the list
                List<String> categories = Arrays.asList("Safety", null);
                Pageable pageable = PageRequest.of(0, 10);
                String authorizationHeader = "Bearer test-token";

                when(categoryRepository.findByName("Safety")).thenReturn(safetyCategory);

                // When & Then
                InvalidPostDataException exception = assertThrows(InvalidPostDataException.class,
                        () -> postService.searchPosts(centerLat, centerLon, radius, keyword, categories,
                                authorizationHeader, pageable));

                assertEquals("Category does not exist: null", exception.getMessage());
                verify(categoryRepository).findByName("Safety");
                verify(categoryRepository).findByName(null);
                verifyNoInteractions(postRepository);
                verifyNoInteractions(jwtService);
        }

        @Test
        void testValidateCategories_AllValid() throws InvalidCredentialsException {
                // Given
                Double centerLat = 0.15;
                Double centerLon = 0.15;
                Double radius = 20.0;
                String keyword = "test";
                // All valid categories
                List<String> categories = Arrays.asList("Safety", "Crime");
                Pageable pageable = PageRequest.of(0, 10);
                String authorizationHeader = "Bearer test-token";
                UUID userId = UUID.randomUUID();

                when(jwtService.getUserIdFromAuthorizationHeader(authorizationHeader)).thenReturn(userId);
                when(categoryRepository.findByName("Safety")).thenReturn(safetyCategory);
                when(categoryRepository.findByName("Crime")).thenReturn(crime);

                List<Post> posts = Arrays.asList(post1, post2);
                Page<Post> postsPage = new PageImpl<>(posts, pageable, posts.size());

                when(postRepository.searchPostsByKeywordAndCategories(any(Point.class), anyDouble(), eq(keyword),
                        eq(categories), eq(pageable))).thenReturn(postsPage);

                // When
                Page<Map<String, Object>> result = postService.searchPosts(
                        centerLat, centerLon, radius, keyword, categories, authorizationHeader, pageable);

                // Then
                assertNotNull(result);
                // Both posts should be in the result as they pass all validation
                assertEquals(2, result.getContent().size());
                verify(categoryRepository).findByName("Safety");
                verify(categoryRepository).findByName("Crime");
                verify(postRepository).searchPostsByKeywordAndCategories(any(Point.class), anyDouble(), eq(keyword),
                        eq(categories), eq(pageable));
                verify(jwtService).getUserIdFromAuthorizationHeader(authorizationHeader);
        }

        @Test
        void testValidateCategories_WhitespaceCategory() {
                // Given
                Double centerLat = 0.15;
                Double centerLon = 0.15;
                Double radius = 20.0;
                String keyword = "test";
                // Include a whitespace-only category
                List<String> categories = Arrays.asList("Safety", "   ");
                Pageable pageable = PageRequest.of(0, 10);
                String authorizationHeader = "Bearer test-token";

                when(categoryRepository.findByName("Safety")).thenReturn(safetyCategory);
                when(categoryRepository.findByName("   ")).thenReturn(null);

                // When & Then
                InvalidPostDataException exception = assertThrows(InvalidPostDataException.class,
                        () -> postService.searchPosts(centerLat, centerLon, radius, keyword, categories,
                                authorizationHeader, pageable));

                assertEquals("Category does not exist:    ", exception.getMessage());
                verify(categoryRepository).findByName("Safety");
                verify(categoryRepository).findByName("   ");
                verifyNoInteractions(postRepository);
                verifyNoInteractions(jwtService);
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
                String authorizationHeader = "Bearer test-token";
                UUID userId = UUID.randomUUID();

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

                when(jwtService.getUserIdFromAuthorizationHeader(authorizationHeader)).thenReturn(userId);
                when(categoryRepository.findByName("Safety")).thenReturn(safetyCategory);
                when(postRepository.findAll()).thenReturn(allPosts);

                // When
                Page<Map<String, Object>> result = postService.findPostsByDistanceFeed(
                        userLat, userLon, categories, keyword, dateFrom, dateTo, authorizationHeader, pageable);

                // Then
                assertNotNull(result);
                assertEquals(1, result.getContent().size()); // Only one post matches all criteria

                Map<String, Object> firstPostResult = result.getContent().getFirst();
                Map<String, Object> firstPostData = (Map<String, Object>) firstPostResult.get("post");
                assertEquals("Test Post", firstPostData.get("title"));
                assertEquals("Safety", firstPostData.get("category"));

                verify(jwtService).getUserIdFromAuthorizationHeader(authorizationHeader);
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
                String authorizationHeader = "Bearer test-token";
                UUID userId = UUID.randomUUID();

                when(jwtService.getUserIdFromAuthorizationHeader(authorizationHeader)).thenReturn(userId);
                when(categoryRepository.findByName("Safety")).thenReturn(safetyCategory);
                when(postRepository.findAll()).thenReturn(Arrays.asList(post1, post2, post3));

                // When
                Page<Map<String, Object>> result = postService.findPostsByDistanceFeed(
                        userLat, userLon, categories, keyword, dateFrom, dateTo, authorizationHeader, pageable);

                // Then
                assertNotNull(result);
                assertTrue(result.getContent().isEmpty());

                verify(jwtService).getUserIdFromAuthorizationHeader(authorizationHeader);
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
                String authorizationHeader = "Bearer test-token";
                UUID userId = UUID.randomUUID();

                // Create sample post with null title but matching keyword in caption
                Post postWithNullTitle = new Post();
                postWithNullTitle.setTitle(null);
                postWithNullTitle.setCaption("This is a test caption");
                postWithNullTitle.setLocation(geometryFactory.createPoint(new Coordinate(0.01, 0.01)));
                postWithNullTitle.setLatitude(0.01);
                postWithNullTitle.setLongitude(0.01);
                postWithNullTitle.setCategory("Safety");
                postWithNullTitle.setCreatedAt(now);

                when(jwtService.getUserIdFromAuthorizationHeader(authorizationHeader)).thenReturn(userId);
                when(categoryRepository.findByName("Safety")).thenReturn(safetyCategory);
                when(postRepository.findAll()).thenReturn(Collections.singletonList(postWithNullTitle));

                // When
                Page<Map<String, Object>> result = postService.findPostsByDistanceFeed(
                        userLat, userLon, categories, keyword, dateFrom, dateTo, authorizationHeader, pageable);

                // Then
                assertNotNull(result);
                assertEquals(1, result.getContent().size()); // Should match on caption

                verify(jwtService).getUserIdFromAuthorizationHeader(authorizationHeader);
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
                String authorizationHeader = "Bearer test-token";
                UUID userId = UUID.randomUUID();

                // Create sample post with null caption but matching keyword in title
                Post postWithNullCaption = new Post();
                postWithNullCaption.setTitle("This is a test title");
                postWithNullCaption.setCaption(null);
                postWithNullCaption.setLocation(geometryFactory.createPoint(new Coordinate(0.01, 0.01)));
                postWithNullCaption.setLatitude(0.01);
                postWithNullCaption.setLongitude(0.01);
                postWithNullCaption.setCategory("Safety");
                postWithNullCaption.setCreatedAt(now);

                when(jwtService.getUserIdFromAuthorizationHeader(authorizationHeader)).thenReturn(userId);
                when(categoryRepository.findByName("Safety")).thenReturn(safetyCategory);
                when(postRepository.findAll()).thenReturn(Collections.singletonList(postWithNullCaption));

                // When
                Page<Map<String, Object>> result = postService.findPostsByDistanceFeed(
                        userLat, userLon, categories, keyword, dateFrom, dateTo, authorizationHeader, pageable);

                // Then
                assertNotNull(result);
                assertEquals(1, result.getContent().size()); // Should match on title

                verify(jwtService).getUserIdFromAuthorizationHeader(authorizationHeader);
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
                String authorizationHeader = "Bearer test-token";
                UUID userId = UUID.randomUUID();

                // Create sample post with null category
                Post postWithNullCategory = new Post();
                postWithNullCategory.setTitle("Test title");
                postWithNullCategory.setCaption("Test caption");
                postWithNullCategory.setLocation(geometryFactory.createPoint(new Coordinate(0.01, 0.01)));
                postWithNullCategory.setLatitude(0.01);
                postWithNullCategory.setLongitude(0.01);
                postWithNullCategory.setCategory(null);
                postWithNullCategory.setCreatedAt(now);

                when(jwtService.getUserIdFromAuthorizationHeader(authorizationHeader)).thenReturn(userId);
                when(categoryRepository.findByName("Safety")).thenReturn(safetyCategory);
                when(postRepository.findAll()).thenReturn(Collections.singletonList(postWithNullCategory));

                // When
                Page<Map<String, Object>> result = postService.findPostsByDistanceFeed(
                        userLat, userLon, categories, keyword, dateFrom, dateTo, authorizationHeader, pageable);

                // Then
                assertNotNull(result);
                assertTrue(result.getContent().isEmpty()); // No match because of category filter

                verify(jwtService).getUserIdFromAuthorizationHeader(authorizationHeader);
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
                String authorizationHeader = "Bearer test-token";
                UUID userId = UUID.randomUUID();

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

                when(jwtService.getUserIdFromAuthorizationHeader(authorizationHeader)).thenReturn(userId);
                when(categoryRepository.findByName("Safety")).thenReturn(safetyCategory);
                when(postRepository.findAll()).thenReturn(Arrays.asList(postInDateRange, postOutsideDateRange));

                // When
                Page<Map<String, Object>> result = postService.findPostsByDistanceFeed(
                        userLat, userLon, categories, keyword, dateFrom, dateTo, authorizationHeader, pageable);

                // Then
                assertNotNull(result);
                assertEquals(1, result.getContent().size()); // Only one post in date range

                verify(jwtService).getUserIdFromAuthorizationHeader(authorizationHeader);
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
                String authorizationHeader = "Bearer test-token";

                when(categoryRepository.findByName("NonexistentCategory")).thenReturn(null);

                // When & Then
                InvalidPostDataException exception = assertThrows(InvalidPostDataException.class, () -> postService
                        .findPostsByDistanceFeed(
                                userLat, userLon, categories, keyword, dateFrom, dateTo,
                                authorizationHeader, pageable));

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
                String authorizationHeader = "Bearer test-token";
                UUID userId = UUID.randomUUID();

                // Create sample post matching all filters
                Post matchingPost = new Post();
                matchingPost.setTitle("Test Post");
                matchingPost.setCaption("Test content");
                matchingPost.setCategory("Safety");
                matchingPost.setCreatedAt(now);

                List<Post> allPosts = Arrays.asList(matchingPost, post1, post2, post3);

                when(jwtService.getUserIdFromAuthorizationHeader(authorizationHeader)).thenReturn(userId);
                when(categoryRepository.findByName("Safety")).thenReturn(safetyCategory);
                when(postRepository.findAll()).thenReturn(allPosts);

                // When
                Page<Map<String, Object>> result = postService.findPostsByTimestampFeed(
                        categories, keyword, dateFrom, dateTo, authorizationHeader, pageable);

                // Then
                assertNotNull(result);
                assertEquals(1, result.getContent().size()); // Only matching post

                Map<String, Object> firstPostResult = result.getContent().getFirst();
                Map<String, Object> firstPostData = (Map<String, Object>) firstPostResult.get("post");
                assertEquals("Test Post", firstPostData.get("title"));
                assertEquals("Safety", firstPostData.get("category"));

                verify(jwtService).getUserIdFromAuthorizationHeader(authorizationHeader);
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
                String authorizationHeader = "Bearer test-token";
                UUID userId = UUID.randomUUID();

                when(jwtService.getUserIdFromAuthorizationHeader(authorizationHeader)).thenReturn(userId);
                when(categoryRepository.findByName("Safety")).thenReturn(safetyCategory);
                when(postRepository.findAll()).thenReturn(Arrays.asList(post1, post2, post3));

                // When
                Page<Map<String, Object>> result = postService.findPostsByTimestampFeed(
                        categories, keyword, dateFrom, dateTo, authorizationHeader, pageable);

                // Then
                assertNotNull(result);
                assertTrue(result.getContent().isEmpty());

                verify(jwtService).getUserIdFromAuthorizationHeader(authorizationHeader);
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
                String authorizationHeader = "Bearer test-token";
                UUID userId = UUID.randomUUID();

                when(jwtService.getUserIdFromAuthorizationHeader(authorizationHeader)).thenReturn(userId);
                when(postRepository.findAll()).thenReturn(Arrays.asList(post1, post2, post3));

                // When
                Page<Map<String, Object>> result = postService.findPostsByTimestampFeed(
                        categories, keyword, dateFrom, dateTo, authorizationHeader, pageable);

                // Then
                assertNotNull(result);
                assertEquals(2, result.getContent().size()); // post1 and post2 (excluding post3)

                verify(jwtService).getUserIdFromAuthorizationHeader(authorizationHeader);
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
                String authorizationHeader = "Bearer test-token";
                UUID userId = UUID.randomUUID();

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

                when(jwtService.getUserIdFromAuthorizationHeader(authorizationHeader)).thenReturn(userId);
                when(postRepository.findAll()).thenReturn(allPosts);

                // When - First page
                Page<Map<String, Object>> firstPageResult = postService.findPostsByTimestampFeed(
                        categories, keyword, dateFrom, dateTo, authorizationHeader, pageable);

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
                        categories, keyword, dateFrom, dateTo, authorizationHeader, secondPageable);

                // Then - Second page
                assertEquals(2, secondPageResult.getContent().size());
                assertEquals(5, secondPageResult.getTotalElements());
                assertEquals(1, secondPageResult.getNumber());
                assertTrue(secondPageResult.hasNext());
                assertTrue(secondPageResult.hasPrevious());

                verify(jwtService, times(2)).getUserIdFromAuthorizationHeader(authorizationHeader);
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
                String authorizationHeader = "Bearer test-token";

                when(categoryRepository.findByName("NonexistentCategory")).thenReturn(null);

                // When & Then
                InvalidPostDataException exception = assertThrows(InvalidPostDataException.class, () -> postService
                        .findPostsByTimestampFeed(
                                categories, keyword, dateFrom, dateTo, authorizationHeader, pageable));

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
                String authorizationHeader = "Bearer test-token";
                UUID userId = UUID.randomUUID();

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

                when(jwtService.getUserIdFromAuthorizationHeader(authorizationHeader)).thenReturn(userId);
                when(postRepository.findAll()).thenReturn(allPosts);

                // When
                Page<Map<String, Object>> result = postService.findPostsByTimestampFeed(
                        categories, keyword, dateFrom, dateTo, authorizationHeader, pageable);

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

                verify(jwtService).getUserIdFromAuthorizationHeader(authorizationHeader);
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
                String authorizationHeader = "Bearer test-token";
                UUID userId = UUID.randomUUID();

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

                when(jwtService.getUserIdFromAuthorizationHeader(authorizationHeader)).thenReturn(userId);
                when(postRepository.findAll()).thenReturn(allPosts);

                // When
                Page<Map<String, Object>> result = postService.findPostsByTimestampFeed(
                        categories, keyword, dateFrom, dateTo, authorizationHeader, pageable);

                // Then
                assertNotNull(result);
                assertEquals(2, result.getContent().size()); // Two posts match the keyword

                verify(jwtService).getUserIdFromAuthorizationHeader(authorizationHeader);
                verify(postRepository).findAll();
        }

        // Direct test for mapPostToData method
        @Test
        void testMapPostToData() {
                // Given
                UUID userId = UUID.randomUUID();
                UUID postId = UUID.randomUUID();
                UUID postedBy = UUID.randomUUID();

                Post post = new Post();
                post.setId(postId);
                post.setTitle("Test Title");
                post.setCaption("Test Caption");
                post.setLocation(geometryFactory.createPoint(new Coordinate(0.1, 0.1)));
                post.setLatitude(0.1);
                post.setLongitude(0.1);
                post.setCategory("Safety");
                post.setCreatedAt(now);
                post.setPostedBy(postedBy);

                // When
                Map<String, Object> result = postService.mapPostToData(post, userId);

                // Then
                assertNotNull(result);
                assertEquals(postId, result.get("id"));
                assertEquals("Test Title", result.get("title"));
                assertEquals("Test Caption", result.get("caption"));
                assertEquals(0.1, result.get("latitude"));
                assertEquals(0.1, result.get("longitude"));
                assertEquals("Safety", result.get("category"));
                assertEquals(now, result.get("createdAt"));
                assertEquals(postedBy, result.get("postedBy"));
                assertEquals(0L, result.get("upvoteCount"));
                assertEquals(0L, result.get("downvoteCount"));
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

        // Test for null title and caption in mapPostToData
        @Test
        void testMapPostToDataWithNullTitleAndCaption() {
                // Given
                UUID userId = UUID.randomUUID();
                UUID postId = UUID.randomUUID();

                Post post = new Post();
                post.setId(postId);
                post.setTitle(null); // Null title
                post.setCaption(null); // Null caption
                post.setLocation(geometryFactory.createPoint(new Coordinate(0.1, 0.1)));
                post.setCategory("Safety");

                // When
                Map<String, Object> result = postService.mapPostToData(post, userId);

                // Then
                assertNotNull(result);
                assertEquals(postId, result.get("id"));
                assertNull(result.get("title"));
                assertNull(result.get("caption"));
        }

        // Test timestamps feed with null parameters
        @Test
        void testFindPostsByTimestampFeed_WithNullAuthHeader() {
                // Given
                Pageable pageable = PageRequest.of(0, 10);
                String authorizationHeader = null; // Null auth header

                // When & Then
                InvalidCredentialsException exception = assertThrows(InvalidCredentialsException.class,
                        () -> postService.findPostsByTimestampFeed(authorizationHeader, pageable));

                assertEquals("Authorization header is required", exception.getMessage());
                verifyNoInteractions(postRepository);
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
                String authorizationHeader = "Bearer test-token";
                UUID userId = UUID.randomUUID();

                when(jwtService.getUserIdFromAuthorizationHeader(authorizationHeader)).thenReturn(userId);
                when(postRepository.findAll()).thenReturn(Arrays.asList(post1, post2, post3));

                // When
                Page<Map<String, Object>> result = postService.findPostsByDistanceFeed(
                        userLat, userLon, categories, keyword, dateFrom, dateTo, authorizationHeader, pageable);

                // Then
                assertNotNull(result);
                assertTrue(result.getContent().isEmpty()); // No posts should match the future dates
                verify(postRepository).findAll();
        }

        // Test for search posts with complex null checking
        @Test
        void testSearchPosts_WithComplexNullChecking() throws InvalidCredentialsException {
                // Given
                Double centerLat = 0.15;
                Double centerLon = 0.15;
                Double radius = 20.0;
                String keyword = "test";
                List<String> categories = null;
                Pageable pageable = PageRequest.of(0, 10);
                String authorizationHeader = "Bearer test-token";
                UUID userId = UUID.randomUUID();

                when(jwtService.getUserIdFromAuthorizationHeader(authorizationHeader)).thenReturn(userId);

                // Create a post with mixed null fields to test null-safety
                Post complexPost = new Post();
                complexPost.setTitle("Test Title");
                complexPost.setCaption(null); // Null caption
                complexPost.setLocation(geometryFactory.createPoint(new Coordinate(0.1, 0.1)));
                complexPost.setLatitude(0.1);
                complexPost.setLongitude(0.1);
                complexPost.setCategory(null); // Null category

                List<Post> posts = Collections.singletonList(complexPost);
                Page<Post> postsPage = new PageImpl<>(posts, pageable, posts.size());

                when(postRepository.searchPostsByKeyword(any(Point.class), anyDouble(), eq(keyword), eq(pageable)))
                        .thenReturn(postsPage);

                // When
                Page<Map<String, Object>> result = postService.searchPosts(
                        centerLat, centerLon, radius, keyword, categories, authorizationHeader, pageable);

                // Then
                assertNotNull(result);
                assertEquals(1, result.getContent().size());

                Map<String, Object> postResult = result.getContent().getFirst();
                Map<String, Object> postData = (Map<String, Object>) postResult.get("post");
                assertNull(postData.get("caption"));
                assertNull(postData.get("category"));

                verify(postRepository).searchPostsByKeyword(any(Point.class), anyDouble(), eq(keyword), eq(pageable));
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
                String authorizationHeader = "Bearer test-token";
                UUID userId = UUID.randomUUID();

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

                when(jwtService.getUserIdFromAuthorizationHeader(authorizationHeader)).thenReturn(userId);
                when(categoryRepository.findByName("Safety")).thenReturn(safetyCategory);
                when(postRepository.findAll()).thenReturn(Arrays.asList(matchingPost1, matchingPost2, nonMatchingPost));

                // When
                Page<Map<String, Object>> result = postService.findPostsByTimestampFeed(
                        categories, keyword, dateFrom, dateTo, authorizationHeader, pageable);

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
                String authorizationHeader = "Bearer test-token";
                UUID userId = UUID.randomUUID();

                when(jwtService.getUserIdFromAuthorizationHeader(authorizationHeader)).thenReturn(userId);
                // Empty list but total elements is 2
                when(postRepository.findAll(pageable)).thenReturn(new PageImpl<>(Collections.emptyList(), pageable, 2));

                // When
                Page<Map<String, Object>> result = postService.findPostsByTimestampFeed(authorizationHeader, pageable);

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

        // Test searchPosts with whitespace-only keyword and empty categories
        @Test
        void testSearchPosts_WithWhitespaceKeywordAndEmptyCategories() {
                // Given
                Double centerLat = 0.15;
                Double centerLon = 0.15;
                Double radius = 20.0;
                String keyword = "   "; // Just whitespace
                List<String> categories = Collections.emptyList(); // Empty list, not null
                Pageable pageable = PageRequest.of(0, 10);
                String authorizationHeader = "Bearer test-token";

                // When
                Page<Map<String, Object>> result = postService.searchPosts(
                        centerLat, centerLon, radius, keyword, categories, authorizationHeader, pageable);

                // Then
                assertNotNull(result);
                assertTrue(result.getContent().isEmpty());
                assertEquals(0, result.getTotalElements());

                // Verify no repository interaction
                verifyNoInteractions(postRepository);
                verifyNoInteractions(jwtService);
        }

        // Test authentication error in findPostsByLocation
        @Test
        void testFindPostsByLocation_AuthenticationError() throws InvalidCredentialsException {
                // Given
                double centerLat = 0.15;
                double centerLon = 0.15;
                double radius = 20.0;
                Pageable pageable = PageRequest.of(0, 10);
                String authorizationHeader = "Bearer invalid-token";
                LocationFilter filter = new LocationFilter(null, null, null);

                when(jwtService.getUserIdFromAuthorizationHeader(authorizationHeader))
                        .thenThrow(new InvalidCredentialsException("Invalid token"));

                // When & Then
                InvalidCredentialsException exception = assertThrows(InvalidCredentialsException.class,
                        () -> postService.findPostsByLocation(
                                centerLat, centerLon, radius, filter, authorizationHeader, pageable));

                assertEquals("Invalid token", exception.getMessage());
                verify(jwtService).getUserIdFromAuthorizationHeader(authorizationHeader);
                verifyNoInteractions(postRepository);
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

        @Test
        void testFindPostsByTimestampFeed_EmptyAuthHeader() {
                // Given
                Pageable pageable = PageRequest.of(0, 10);
                String authorizationHeader = ""; // Empty auth header

                // When & Then
                InvalidCredentialsException exception = assertThrows(InvalidCredentialsException.class,
                        () -> postService.findPostsByTimestampFeed(authorizationHeader, pageable));

                assertEquals("Authorization header is required", exception.getMessage());
                verifyNoInteractions(postRepository);
        }

        @Test
        void testFindPostsByTimestampFeed_SortingByTimestamp() throws InvalidCredentialsException {
                // Given
                Pageable pageable = PageRequest.of(0, 10);
                String authorizationHeader = "Bearer test-token";
                UUID userId = UUID.randomUUID();

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

                when(jwtService.getUserIdFromAuthorizationHeader(authorizationHeader)).thenReturn(userId);
                when(postRepository.findAll(pageable)).thenReturn(postsPage);

                // When
                Page<Map<String, Object>> result = postService.findPostsByTimestampFeed(authorizationHeader, pageable);

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

                verify(jwtService).getUserIdFromAuthorizationHeader(authorizationHeader);
                verify(postRepository).findAll(pageable);
        }

        @Test
        void testFindPostsByTimestampFeed_PaginationParameters() throws InvalidCredentialsException {
                // Given
                int pageSize = 2;
                int pageNumber = 1; // Second page
                Pageable pageable = PageRequest.of(pageNumber, pageSize);
                String authorizationHeader = "Bearer test-token";
                UUID userId = UUID.randomUUID();

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

                when(jwtService.getUserIdFromAuthorizationHeader(authorizationHeader)).thenReturn(userId);
                when(postRepository.findAll(pageable)).thenReturn(postsPage);

                // When
                Page<Map<String, Object>> result = postService.findPostsByTimestampFeed(authorizationHeader, pageable);

                // Then
                assertNotNull(result);
                assertEquals(2, result.getContent().size());
                assertEquals(5, result.getTotalElements());
                assertEquals(3, result.getTotalPages());
                assertEquals(1, result.getNumber()); // Page number (0-based)
                assertTrue(result.hasNext());
                assertTrue(result.hasPrevious());

                verify(jwtService).getUserIdFromAuthorizationHeader(authorizationHeader);
                verify(postRepository).findAll(pageable);
        }
}