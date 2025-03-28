package com.safetypin.post.controller;

import com.safetypin.post.dto.PostCreateRequest;
import com.safetypin.post.dto.PostResponse;
import com.safetypin.post.dto.LocationFilter;
import com.safetypin.post.exception.InvalidPostDataException;
import com.safetypin.post.exception.PostNotFoundException;
import com.safetypin.post.model.Category;
import com.safetypin.post.model.Post;
import com.safetypin.post.service.JwtService;
import com.safetypin.post.service.PostService;
import org.apache.hc.client5.http.auth.InvalidCredentialsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostControllerTest {

        @Mock
        private PostService postService;

        @InjectMocks
        private PostController postController;
        @Mock
        private JwtService jwtService;

        private Page<Map<String, Object>> mockPage;
        private Post mockPost;
        private PostCreateRequest validRequest;
        private String authorizationHeader;
        private UUID testUserId;

        @BeforeEach
        void setUp() {
                mockPage = new PageImpl<>(new ArrayList<>());
                authorizationHeader = "Bearer test-token";
                testUserId = UUID.randomUUID();

                Category mockCategory = new Category();
                mockCategory.setName("safety");

                mockPost = new Post();
                mockPost.setId(UUID.randomUUID());
                mockPost.setTitle("Test Post");
                mockPost.setCaption("Test Caption");
                mockPost.setLatitude(20.0);
                mockPost.setLongitude(10.0);
                mockPost.setCategory(mockCategory.getName());
                mockPost.setPostedBy(testUserId);

                validRequest = new PostCreateRequest();
                validRequest.setTitle("Test Post");
                validRequest.setCaption("Test Caption");
                validRequest.setLatitude(20.0);
                validRequest.setLongitude(10.0);
                validRequest.setCategory(mockCategory.getName());
                validRequest.setPostedBy(testUserId); // Add postedBy to valid request
        }

        /**
         * Test MethodArgumentTypeMismatchException handler
         */
        @Test
        void handleArgumentTypeMismatchException_returnsCorrectErrorResponse() {
                MethodArgumentTypeMismatchException ex = mock(MethodArgumentTypeMismatchException.class);
                ResponseEntity<PostResponse> response = postController.handleArgumentTypeMismatch(ex);

                assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
                PostResponse errorResponse = response.getBody();

                assertNotNull(errorResponse);
                assertEquals("Invalid location parameters", errorResponse.getMessage());
        }

        @Test
        void createPost_Success() {
                try {
                        doReturn(testUserId).when(jwtService).getUserIdFromAuthorizationHeader(authorizationHeader);
                } catch (InvalidCredentialsException e) {
                        fail(e.getMessage());
                }
                when(postService.createPost(
                                anyString(), anyString(), anyDouble(), anyDouble(), anyString(), any(UUID.class)))
                                .thenReturn(mockPost);

                ResponseEntity<PostResponse> response = postController.createPost(authorizationHeader, validRequest);

                assertEquals(HttpStatus.CREATED, response.getStatusCode());
                assertTrue(Objects.requireNonNull(response.getBody()).isSuccess());
                assertEquals("Post created successfully", response.getBody().getMessage());
                assertNotNull(response.getBody().getData());
        }

        @Test
        void createPost_ExceptionThrown() {
                try {
                        doReturn(testUserId).when(jwtService).getUserIdFromAuthorizationHeader(authorizationHeader);
                } catch (InvalidCredentialsException e) {
                        fail(e.getMessage());
                }
                when(postService.createPost(
                                anyString(), anyString(), anyDouble(), anyDouble(), anyString(), any(UUID.class)))
                                .thenThrow(new InvalidPostDataException("Test exception"));

                ResponseEntity<PostResponse> response = postController.createPost(authorizationHeader, validRequest);

                assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
                assertFalse(Objects.requireNonNull(response.getBody()).isSuccess());
                assertEquals("Test exception", response.getBody().getMessage());
                assertNull(response.getBody().getData());
        }

        @Test
        void createPost_RuntimeException() {
                try {
                        doReturn(testUserId).when(jwtService).getUserIdFromAuthorizationHeader(authorizationHeader);
                } catch (InvalidCredentialsException e) {
                        fail(e.getMessage());
                }
                when(postService.createPost(
                                anyString(), anyString(), anyDouble(), anyDouble(), anyString(), any(UUID.class)))
                                .thenThrow(new RuntimeException("Unexpected runtime exception"));

                ResponseEntity<PostResponse> response = postController.createPost(authorizationHeader, validRequest);

                assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
                assertFalse(Objects.requireNonNull(response.getBody()).isSuccess());
                assertEquals("Error processing request: Unexpected runtime exception", response.getBody().getMessage());
                assertNull(response.getBody().getData());
        }

        @Test
        void createPost_ExceptionThrown2() {
                try {
                        doThrow(new InvalidCredentialsException("Exception at JwtService")).when(jwtService)
                                        .getUserIdFromAuthorizationHeader(authorizationHeader);
                } catch (InvalidCredentialsException e) {
                        fail(e.getMessage());
                }

                ResponseEntity<PostResponse> response = postController.createPost(authorizationHeader, validRequest);

                assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
                assertFalse(Objects.requireNonNull(response.getBody()).isSuccess());
                assertEquals("Authentication failed: Exception at JwtService", response.getBody().getMessage());
                assertNull(response.getBody().getData());
        }

        /**
         * Test for successful retrieval of all posts
         */
        @Test
        void findAll_Success() {
                int page = 0;
                int size = 10;
                Pageable pageable = PageRequest.of(page, size);

                List<Post> postList = new ArrayList<>();
                postList.add(mockPost);
                Page<Post> postPage = new PageImpl<>(postList, pageable, 1);

                when(postService.findAllPaginated(authorizationHeader, pageable)).thenReturn(postPage);

                ResponseEntity<PostResponse> response = postController.findAll(authorizationHeader, page, size);

                assertEquals(HttpStatus.OK, response.getStatusCode());
                PostResponse postResponse = response.getBody();
                assert postResponse != null;
                assertTrue(postResponse.isSuccess());

                @SuppressWarnings("unchecked")
                Map<String, Object> responseData = (Map<String, Object>) postResponse.getData();
                assertEquals(1L, responseData.get("totalElements"));
                assertEquals(0, responseData.get("currentPage"));
                assertEquals(10, responseData.get("pageSize"));
        }

        /**
         * Test for exception during retrieval of all posts
         */
        @Test
        void findAll_ExceptionThrown() {
                int page = 0;
                int size = 10;
                String errorMessage = "Database error";

                when(postService.findAllPaginated(anyString(), any(Pageable.class)))
                                .thenThrow(new RuntimeException(errorMessage));

                ResponseEntity<PostResponse> response = postController.findAll(authorizationHeader, page, size);

                assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
                PostResponse errorResponse = response.getBody();
                assertNotNull(errorResponse);
                assertFalse(errorResponse.isSuccess());
                assertEquals("Error processing request: " + errorMessage, errorResponse.getMessage());
                assertNull(errorResponse.getData());
        }

        /**
         * Test successful retrieval of post by ID
         */
        @Test
        void getPostById_Success() {
                // Arrange
                UUID postId = UUID.randomUUID();
                when(postService.findById(postId)).thenReturn(mockPost);

                // Act
                ResponseEntity<PostResponse> response = postController.getPostById(postId);

                // Assert
                assertEquals(HttpStatus.OK, response.getStatusCode());
                PostResponse postResponse = response.getBody();
                assertNotNull(postResponse);
                assertTrue(postResponse.isSuccess());
                assertNotNull(postResponse.getData());
                verify(postService).findById(postId);
        }

        /**
         * Test post not found exception
         */
        @Test
        void getPostById_PostNotFound() {
                // Arrange
                UUID postId = UUID.randomUUID();
                when(postService.findById(postId)).thenThrow(new PostNotFoundException("Post not found"));

                // Act
                ResponseEntity<PostResponse> response = postController.getPostById(postId);

                // Assert
                assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
                PostResponse errorResponse = response.getBody();
                assertNotNull(errorResponse);
                assertFalse(errorResponse.isSuccess());
                assertEquals("Post not found", errorResponse.getMessage());
                assertNull(errorResponse.getData());
        }

        /**
         * Test generic exception handling
         */
        @Test
        void getPostById_GenericException() {
                // Arrange
                UUID postId = UUID.randomUUID();
                String errorMessage = "Database error";
                when(postService.findById(postId)).thenThrow(new RuntimeException(errorMessage));

                // Act
                ResponseEntity<PostResponse> response = postController.getPostById(postId);

                // Assert
                assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
                PostResponse errorResponse = response.getBody();
                assertNotNull(errorResponse);
                assertFalse(errorResponse.isSuccess());
                assertEquals("Error processing request: " + errorMessage, errorResponse.getMessage());
                assertNull(errorResponse.getData());
        }

        /**
         * Test successful posts feed by distance
         */
        @Test
        void getPostsFeedByDistance_Success() throws InvalidCredentialsException {
                // Arrange
                Double lat = 20.0;
                Double lon = 10.0;
                List<String> categories = List.of("safety");
                String keyword = "test";
                LocalDate dateFrom = LocalDate.now().minusDays(7);
                LocalDate dateTo = LocalDate.now();
                int page = 0;
                int size = 10;
                Pageable pageable = PageRequest.of(page, size);

                LocalDateTime fromDateTime = LocalDateTime.of(dateFrom, LocalTime.MIN);
                LocalDateTime toDateTime = LocalDateTime.of(dateTo, LocalTime.MAX);

                when(postService.findPostsByDistanceFeed(
                                lat, lon, categories, keyword,
                                fromDateTime, toDateTime, authorizationHeader, pageable))
                                .thenReturn(mockPage);

                // Act
                ResponseEntity<PostResponse> response = postController.getPostsFeedByDistance(
                                authorizationHeader, lat, lon, categories, keyword, dateFrom, dateTo, page, size);

                // Assert
                assertEquals(HttpStatus.OK, response.getStatusCode());
                assertTrue(Objects.requireNonNull(response.getBody()).isSuccess());
                verify(postService).findPostsByDistanceFeed(
                                lat, lon, categories, keyword,
                                fromDateTime, toDateTime, authorizationHeader, pageable);
        }

        /**
         * Test posts feed by distance with missing latitude
         */
        @Test
        void getPostsFeedByDistance_MissingLatitude() {
                // Act
                ResponseEntity<PostResponse> response = postController.getPostsFeedByDistance(
                                authorizationHeader, null, 10.0, null, "test", null, null, 0, 10);

                // Assert
                assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
                PostResponse errorResponse = response.getBody();
                assertNotNull(errorResponse);
                assertFalse(errorResponse.isSuccess());
                assertEquals("Latitude and longitude are required", errorResponse.getMessage());
                verifyNoInteractions(postService);
        }

        /**
         * Test posts feed by distance with missing longitude
         */
        @Test
        void getPostsFeedByDistance_MissingLongitude() {
                // Act
                ResponseEntity<PostResponse> response = postController.getPostsFeedByDistance(
                                authorizationHeader, 20.0, null, null, "test", null, null, 0, 10);

                // Assert
                assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
                PostResponse errorResponse = response.getBody();
                assertNotNull(errorResponse);
                assertFalse(errorResponse.isSuccess());
                assertEquals("Latitude and longitude are required", errorResponse.getMessage());
                verifyNoInteractions(postService);
        }

        /**
         * Test successful posts feed by timestamp
         */
        @Test
        void getPostsFeedByTimestamp_Success() throws InvalidCredentialsException {
                // Arrange
                List<String> categories = List.of("safety");
                String keyword = "test";
                LocalDate dateFrom = LocalDate.now().minusDays(7);
                LocalDate dateTo = LocalDate.now();
                int page = 0;
                int size = 10;
                Pageable pageable = PageRequest.of(page, size);

                LocalDateTime fromDateTime = LocalDateTime.of(dateFrom, LocalTime.MIN);
                LocalDateTime toDateTime = LocalDateTime.of(dateTo, LocalTime.MAX);

                when(postService.findPostsByTimestampFeed(
                                categories, keyword, fromDateTime, toDateTime,
                                authorizationHeader, pageable))
                                .thenReturn(mockPage);

                // Act
                ResponseEntity<PostResponse> response = postController.getPostsFeedByTimestamp(
                                authorizationHeader, categories, keyword, dateFrom, dateTo, page, size);

                // Assert
                assertEquals(HttpStatus.OK, response.getStatusCode());
                assertTrue(Objects.requireNonNull(response.getBody()).isSuccess());
                verify(postService).findPostsByTimestampFeed(
                                categories, keyword, fromDateTime, toDateTime,
                                authorizationHeader, pageable);
        }

        /**
         * Test posts feed by distance with only categories
         */
        @Test
        void getPostsFeedByDistance_OnlyCategories() throws InvalidCredentialsException {
                // Arrange
                Double lat = 20.0;
                Double lon = 10.0;
                List<String> categories = List.of("safety", "emergency");
                int page = 0;
                int size = 10;
                Pageable pageable = PageRequest.of(page, size);

                when(postService.findPostsByDistanceFeed(
                                eq(lat), eq(lon), eq(categories), isNull(),
                                isNull(), isNull(), eq(authorizationHeader), eq(pageable)))
                                .thenReturn(mockPage);

                // Act
                ResponseEntity<PostResponse> response = postController.getPostsFeedByDistance(
                                authorizationHeader, lat, lon, categories, null, null, null, page, size);

                // Assert
                assertEquals(HttpStatus.OK, response.getStatusCode());
                assertTrue(Objects.requireNonNull(response.getBody()).isSuccess());
                verify(postService).findPostsByDistanceFeed(
                                eq(lat), eq(lon), eq(categories), isNull(),
                                isNull(), isNull(), eq(authorizationHeader), eq(pageable));
        }

        /**
         * Test posts feed by distance with only keyword
         */
        @Test
        void getPostsFeedByDistance_OnlyKeyword() throws InvalidCredentialsException {
                // Arrange
                Double lat = 20.0;
                Double lon = 10.0;
                String keyword = "danger";
                int page = 0;
                int size = 10;
                Pageable pageable = PageRequest.of(page, size);

                when(postService.findPostsByDistanceFeed(
                                eq(lat), eq(lon), isNull(), eq(keyword),
                                isNull(), isNull(), eq(authorizationHeader), eq(pageable)))
                                .thenReturn(mockPage);

                // Act
                ResponseEntity<PostResponse> response = postController.getPostsFeedByDistance(
                                authorizationHeader, lat, lon, null, keyword, null, null, page, size);

                // Assert
                assertEquals(HttpStatus.OK, response.getStatusCode());
                assertTrue(Objects.requireNonNull(response.getBody()).isSuccess());
                verify(postService).findPostsByDistanceFeed(
                                eq(lat), eq(lon), isNull(), eq(keyword),
                                isNull(), isNull(), eq(authorizationHeader), eq(pageable));
        }

        /**
         * Test posts feed by timestamp with null categories and keyword
         */
        @Test
        void getPostsFeedByTimestamp_NullCategoriesAndKeyword() throws InvalidCredentialsException {
                // Arrange
                int page = 0;
                int size = 10;
                Pageable pageable = PageRequest.of(page, size);

                when(postService.findPostsByTimestampFeed(
                                isNull(), isNull(), isNull(), isNull(),
                                eq(authorizationHeader), eq(pageable)))
                                .thenReturn(mockPage);

                // Act
                ResponseEntity<PostResponse> response = postController.getPostsFeedByTimestamp(
                                authorizationHeader, null, null, null, null, page, size);

                // Assert
                assertEquals(HttpStatus.OK, response.getStatusCode());
                assertTrue(Objects.requireNonNull(response.getBody()).isSuccess());
                verify(postService).findPostsByTimestampFeed(
                                isNull(), isNull(), isNull(), isNull(),
                                eq(authorizationHeader), eq(pageable));
        }

        /**
         * Test posts feed by timestamp with only categories
         */
        @Test
        void getPostsFeedByTimestamp_OnlyCategories() throws InvalidCredentialsException {
                // Arrange
                List<String> categories = List.of("safety", "emergency");
                int page = 0;
                int size = 10;
                Pageable pageable = PageRequest.of(page, size);

                when(postService.findPostsByTimestampFeed(
                                eq(categories), isNull(), isNull(), isNull(),
                                eq(authorizationHeader), eq(pageable)))
                                .thenReturn(mockPage);

                // Act
                ResponseEntity<PostResponse> response = postController.getPostsFeedByTimestamp(
                                authorizationHeader, categories, null, null, null, page, size);

                // Assert
                assertEquals(HttpStatus.OK, response.getStatusCode());
                assertTrue(Objects.requireNonNull(response.getBody()).isSuccess());
                verify(postService).findPostsByTimestampFeed(
                                eq(categories), isNull(), isNull(), isNull(),
                                eq(authorizationHeader), eq(pageable));
        }

        /**
         * Test posts feed by timestamp with only keyword
         */
        @Test
        void getPostsFeedByTimestamp_OnlyKeyword() throws InvalidCredentialsException {
                // Arrange
                String keyword = "danger";
                int page = 0;
                int size = 10;
                Pageable pageable = PageRequest.of(page, size);

                when(postService.findPostsByTimestampFeed(
                                isNull(), eq(keyword), isNull(), isNull(),
                                eq(authorizationHeader), eq(pageable)))
                                .thenReturn(mockPage);

                // Act
                ResponseEntity<PostResponse> response = postController.getPostsFeedByTimestamp(
                                authorizationHeader, null, keyword, null, null, page, size);

                // Assert
                assertEquals(HttpStatus.OK, response.getStatusCode());
                assertTrue(Objects.requireNonNull(response.getBody()).isSuccess());
                verify(postService).findPostsByTimestampFeed(
                                isNull(), eq(keyword), isNull(), isNull(),
                                eq(authorizationHeader), eq(pageable));
        }

        /**
         * Test successful post deletion
         */
        @Test
        void deletePost_Success() throws InvalidCredentialsException {
                // Arrange
                UUID postId = UUID.randomUUID();

                // Mock JWT service to return a valid user ID
                when(jwtService.getUserIdFromAuthorizationHeader(authorizationHeader))
                                .thenReturn(testUserId);

                // Mock post service to not throw exceptions (successful deletion)
                doNothing().when(postService).deletePost(postId, testUserId);

                // Act
                ResponseEntity<PostResponse> response = postController.deletePost(authorizationHeader, postId);

                // Assert
                assertEquals(HttpStatus.OK, response.getStatusCode());
                PostResponse postResponse = response.getBody();
                assertNotNull(postResponse);
                assertTrue(postResponse.isSuccess());
                assertEquals("Post deleted successfully", postResponse.getMessage());

                // Verify the response data contains the expected info
                @SuppressWarnings("unchecked")
                Map<String, Object> responseData = (Map<String, Object>) postResponse.getData();
                assertEquals(postId, responseData.get("postId"));
                assertEquals(testUserId, responseData.get("deletedBy"));

                // Verify the service was called with correct parameters
                verify(postService).deletePost(postId, testUserId);
                verify(jwtService).getUserIdFromAuthorizationHeader(authorizationHeader);
        }

        /**
         * Test post deletion with JWT authentication failure
         */
        @Test
        void deletePost_AuthenticationFailure() throws InvalidCredentialsException {
                // Arrange
                UUID postId = UUID.randomUUID();
                String errorMessage = "Invalid token";

                // Mock JWT service to throw an exception
                when(jwtService.getUserIdFromAuthorizationHeader(authorizationHeader))
                                .thenThrow(new InvalidCredentialsException(errorMessage));

                // Act
                ResponseEntity<PostResponse> response = postController.deletePost(authorizationHeader, postId);

                // Assert
                assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
                PostResponse errorResponse = response.getBody();
                assertNotNull(errorResponse);
                assertFalse(errorResponse.isSuccess());
                assertEquals("Authentication failed: " + errorMessage, errorResponse.getMessage());

                // Verify post service was not called
                verifyNoInteractions(postService);
        }

        /**
         * Test post deletion when post not found
         */
        @Test
        void deletePost_PostNotFound() throws InvalidCredentialsException {
                // Arrange
                UUID postId = UUID.randomUUID();
                String errorMessage = "Post not found";

                // Mock JWT service to return a valid user ID
                when(jwtService.getUserIdFromAuthorizationHeader(authorizationHeader))
                                .thenReturn(testUserId);

                // Mock post service to throw PostNotFoundException
                doThrow(new PostNotFoundException(errorMessage))
                                .when(postService).deletePost(postId, testUserId);

                // Act
                ResponseEntity<PostResponse> response = postController.deletePost(authorizationHeader, postId);

                // Assert
                assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
                PostResponse errorResponse = response.getBody();
                assertNotNull(errorResponse);
                assertFalse(errorResponse.isSuccess());
                assertEquals(errorMessage, errorResponse.getMessage());
        }

        /**
         * Test post deletion with general error
         */
        @Test
        void deletePost_GeneralError() throws InvalidCredentialsException {
                // Arrange
                UUID postId = UUID.randomUUID();
                String errorMessage = "Database connection error";

                // Mock JWT service to return a valid user ID
                when(jwtService.getUserIdFromAuthorizationHeader(authorizationHeader))
                                .thenReturn(testUserId);

                // Mock post service to throw generic RuntimeException
                doThrow(new RuntimeException(errorMessage))
                                .when(postService).deletePost(postId, testUserId);

                // Act
                ResponseEntity<PostResponse> response = postController.deletePost(authorizationHeader, postId);

                // Assert
                assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
                PostResponse errorResponse = response.getBody();
                assertNotNull(errorResponse);
                assertFalse(errorResponse.isSuccess());
                assertEquals("Error processing request: " + errorMessage, errorResponse.getMessage());
        }

        /**
         * Test authentication failure in feed by distance endpoint
         */
        @Test
        void getPostsFeedByDistance_AuthenticationFailure() throws InvalidCredentialsException {
                // Arrange
                Double lat = 20.0;
                Double lon = 10.0;
                List<String> categories = List.of("safety");
                String keyword = "test";
                LocalDate dateFrom = LocalDate.now().minusDays(7);
                LocalDate dateTo = LocalDate.now();
                int page = 0;
                int size = 10;
                String errorMessage = "Invalid token";

                // Mock the service to throw InvalidCredentialsException
                when(postService.findPostsByDistanceFeed(
                                anyDouble(), anyDouble(), anyList(), anyString(),
                                any(LocalDateTime.class), any(LocalDateTime.class),
                                anyString(), any(Pageable.class)))
                                .thenThrow(new InvalidCredentialsException(errorMessage));

                // Act
                ResponseEntity<PostResponse> response = postController.getPostsFeedByDistance(
                                authorizationHeader, lat, lon, categories, keyword, dateFrom, dateTo, page, size);

                // Assert
                assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
                PostResponse errorResponse = response.getBody();
                assertNotNull(errorResponse);
                assertFalse(errorResponse.isSuccess());
                assertEquals("Authentication failed: " + errorMessage, errorResponse.getMessage());
                assertNull(errorResponse.getData());

                // Verify the service method was called
                verify(postService).findPostsByDistanceFeed(
                                eq(lat), eq(lon), eq(categories), eq(keyword),
                                any(LocalDateTime.class), any(LocalDateTime.class),
                                eq(authorizationHeader), any(Pageable.class));
        }

        /**
         * Test authentication failure in feed by timestamp endpoint
         */
        @Test
        void getPostsFeedByTimestamp_AuthenticationFailure() throws InvalidCredentialsException {
                // Arrange
                List<String> categories = List.of("safety");
                String keyword = "test";
                LocalDate dateFrom = LocalDate.now().minusDays(7);
                LocalDate dateTo = LocalDate.now();
                int page = 0;
                int size = 10;
                String errorMessage = "Invalid token";

                // Mock the service to throw InvalidCredentialsException
                when(postService.findPostsByTimestampFeed(
                                anyList(), anyString(), any(LocalDateTime.class), any(LocalDateTime.class),
                                anyString(), any(Pageable.class)))
                                .thenThrow(new InvalidCredentialsException(errorMessage));

                // Act
                ResponseEntity<PostResponse> response = postController.getPostsFeedByTimestamp(
                                authorizationHeader, categories, keyword, dateFrom, dateTo, page, size);

                // Assert
                assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
                PostResponse errorResponse = response.getBody();
                assertNotNull(errorResponse);
                assertFalse(errorResponse.isSuccess());
                assertEquals("Authentication failed: " + errorMessage, errorResponse.getMessage());
                assertNull(errorResponse.getData());

                // Verify the service method was called
                verify(postService).findPostsByTimestampFeed(
                                eq(categories), eq(keyword),
                                any(LocalDateTime.class), any(LocalDateTime.class),
                                eq(authorizationHeader), any(Pageable.class));
        }
}