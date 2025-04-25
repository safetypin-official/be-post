package com.safetypin.post.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.safetypin.post.dto.PostedByData;
import com.safetypin.post.dto.UserProfileBatchRequest;
import com.safetypin.post.dto.UserProfileBatchResponse;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceImplTest {

    // Define fixed UUIDs for testing
    private static final UUID USER_ID_1 = UUID.fromString("8ddb4096-94bc-495d-9687-99c21aa21846");
    private static final UUID USER_ID_2 = UUID.fromString("0db2ec66-bf9b-4fec-93b4-10886986b0fe");
    private static MockWebServer mockWebServer;
    private UserProfileService userProfileService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeAll
    static void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
    }

    @AfterAll
    static void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @BeforeEach
    void initialize() {
        String baseUrl = String.format("http://localhost:%s", mockWebServer.getPort());
        // Use the test constructor
        userProfileService = new UserProfileServiceImpl(WebClient.builder().baseUrl(baseUrl).build(), baseUrl);
    }

    @Test
    void fetchUserProfilesBatch_success() throws JsonProcessingException {
        // Arrange
        // Use fixed UUIDs
        PostedByData profile1 = PostedByData.builder().userId(USER_ID_1).name("User One").profilePicture("pic1.jpg")
                .build();
        PostedByData profile2 = PostedByData.builder().userId(USER_ID_2).name("User Two").profilePicture("pic2.jpg")
                .build();
        UserProfileBatchResponse mockResponseDto = new UserProfileBatchResponse(List.of(profile1, profile2));

        mockWebServer.enqueue(
                new MockResponse()
                        .setBody(objectMapper.writeValueAsString(mockResponseDto))
                        .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));

        // Use fixed UUIDs
        List<UUID> userIds = List.of(USER_ID_1, USER_ID_2);

        // Act
        Mono<Map<UUID, PostedByData>> resultMono = userProfileService.fetchUserProfilesBatch(userIds);

        // Assert
        StepVerifier.create(resultMono)
                .assertNext(profileMap -> {
                    assertNotNull(profileMap);
                    assertEquals(2, profileMap.size());
                    // Use fixed UUIDs for assertion - check keys and content
                    assertTrue(profileMap.containsKey(USER_ID_1));
                    assertEquals(profile1.getName(), profileMap.get(USER_ID_1).getName());
                    assertEquals(profile1.getProfilePicture(), profileMap.get(USER_ID_1).getProfilePicture());

                    assertTrue(profileMap.containsKey(USER_ID_2));
                    assertEquals(profile2.getName(), profileMap.get(USER_ID_2).getName());
                    assertEquals(profile2.getProfilePicture(), profileMap.get(USER_ID_2).getProfilePicture());
                })
                .verifyComplete();

        // Verify request
        try {
            var recordedRequest = mockWebServer.takeRequest(1, TimeUnit.SECONDS); // Added timeout
            assertNotNull(recordedRequest); // Ensure request was made
            assertEquals("/api/profiles/batch", recordedRequest.getPath());
            assertEquals("POST", recordedRequest.getMethod());
            UserProfileBatchRequest requestBody = objectMapper.readValue(recordedRequest.getBody().readUtf8(),
                    UserProfileBatchRequest.class);
            // Use fixed UUIDs for assertion
            assertEquals(userIds, requestBody.getUserIds());
        } catch (InterruptedException | IOException e) {
            fail("Failed to take request from mock server", e);
        }
    }

    @Test
    void fetchUserProfilesBatch_withDuplicateInputIds_shouldRequestDistinctIds() throws JsonProcessingException {
        // Arrange
        // Use fixed UUIDs
        PostedByData profile1 = PostedByData.builder().userId(USER_ID_1).name("User One").profilePicture("pic1.jpg")
                .build();
        PostedByData profile2 = PostedByData.builder().userId(USER_ID_2).name("User Two").profilePicture("pic2.jpg")
                .build();
        UserProfileBatchResponse mockResponseDto = new UserProfileBatchResponse(List.of(profile1, profile2));

        mockWebServer.enqueue(
                new MockResponse()
                        .setBody(objectMapper.writeValueAsString(mockResponseDto))
                        .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));

        // Use fixed UUIDs, including a duplicate
        List<UUID> userIdsWithDuplicates = List.of(USER_ID_1, USER_ID_2, USER_ID_1);

        // Act
        Mono<Map<UUID, PostedByData>> resultMono = userProfileService.fetchUserProfilesBatch(userIdsWithDuplicates);

        // Assert
        StepVerifier.create(resultMono)
                .assertNext(profileMap -> {
                    assertNotNull(profileMap);
                    assertEquals(2, profileMap.size()); // Still expects 2 profiles back
                    // Use fixed UUIDs for assertion - check keys and content
                    assertTrue(profileMap.containsKey(USER_ID_1));
                    assertEquals(profile1.getName(), profileMap.get(USER_ID_1).getName());
                    assertEquals(profile1.getProfilePicture(), profileMap.get(USER_ID_1).getProfilePicture());

                    assertTrue(profileMap.containsKey(USER_ID_2));
                    assertEquals(profile2.getName(), profileMap.get(USER_ID_2).getName());
                    assertEquals(profile2.getProfilePicture(), profileMap.get(USER_ID_2).getProfilePicture());
                })
                .verifyComplete();

        // Verify request body contains only distinct IDs
        try {
            var recordedRequest = mockWebServer.takeRequest(1, TimeUnit.SECONDS); // Added timeout
            assertNotNull(recordedRequest); // Ensure request was made
            assertEquals("/api/profiles/batch", recordedRequest.getPath());
            UserProfileBatchRequest requestBody = objectMapper.readValue(recordedRequest.getBody().readUtf8(),
                    UserProfileBatchRequest.class);
            // Use fixed UUIDs for assertion - should only contain distinct IDs
            assertEquals(List.of(USER_ID_1, USER_ID_2), requestBody.getUserIds());
        } catch (InterruptedException | IOException e) {
            fail("Failed to take request from mock server", e);
        }
    }

    @Test
    void fetchUserProfilesBatch_emptyInputList_returnsEmptyMap() {
        // Arrange
        List<UUID> userIds = Collections.emptyList();

        // Act
        Mono<Map<UUID, PostedByData>> resultMono = userProfileService.fetchUserProfilesBatch(userIds);

        // Assert
        StepVerifier.create(resultMono)
                .expectNext(Collections.emptyMap())
                .verifyComplete();
        // Verify no request was made for this specific test
        try {
            assertNull(mockWebServer.takeRequest(0, TimeUnit.MILLISECONDS));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail("Test interrupted while checking for request", e);
        }
    }

    @Test
    void fetchUserProfilesBatch_nullInputList_returnsEmptyMap() {
        // Act
        Mono<Map<UUID, PostedByData>> resultMono = userProfileService.fetchUserProfilesBatch(null);

        // Assert
        StepVerifier.create(resultMono)
                .expectNext(Collections.emptyMap())
                .verifyComplete();
        // Verify no request was made for this specific test
        try {
            assertNull(mockWebServer.takeRequest(0, TimeUnit.MILLISECONDS));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail("Test interrupted while checking for request", e);
        }
    }

    @Test
    void fetchUserProfilesBatch_apiReturnsEmptyList_returnsEmptyMap() throws JsonProcessingException {
        // Arrange
        UserProfileBatchResponse mockResponseDto = new UserProfileBatchResponse(Collections.emptyList());
        mockWebServer.enqueue(
                new MockResponse()
                        .setBody(objectMapper.writeValueAsString(mockResponseDto))
                        .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));

        List<UUID> userIds = List.of(UUID.randomUUID());

        // Act
        Mono<Map<UUID, PostedByData>> resultMono = userProfileService.fetchUserProfilesBatch(userIds);

        // Assert
        StepVerifier.create(resultMono)
                .expectNext(Collections.emptyMap())
                .verifyComplete();

        // Verify request was made
        try {
            assertNotNull(mockWebServer.takeRequest(1, TimeUnit.SECONDS));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail("Test interrupted", e);
        }
    }

    @Test
    void fetchUserProfilesBatch_apiReturnsNullProfileList_returnsEmptyMap() throws JsonProcessingException {
        // Arrange
        UserProfileBatchResponse mockResponseDto = new UserProfileBatchResponse(null); // Null list
        mockWebServer.enqueue(
                new MockResponse()
                        .setBody(objectMapper.writeValueAsString(mockResponseDto))
                        .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));

        List<UUID> userIds = List.of(UUID.randomUUID());

        // Act
        Mono<Map<UUID, PostedByData>> resultMono = userProfileService.fetchUserProfilesBatch(userIds);

        // Assert
        StepVerifier.create(resultMono)
                .expectNext(Collections.emptyMap())
                .verifyComplete();
        // Verify request was made
        try {
            assertNotNull(mockWebServer.takeRequest(1, TimeUnit.SECONDS));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail("Test interrupted", e);
        }
    }

    @Test
    void fetchUserProfilesBatch_apiReturnsProfileWithNullId_filtersOut() throws JsonProcessingException {
        // Arrange
        UUID userId1 = UUID.randomUUID();
        PostedByData profile1 = PostedByData.builder().userId(userId1).name("User One").profilePicture("pic1.jpg")
                .build();
        PostedByData profileNullId = PostedByData.builder().userId(null).name("No ID User").profilePicture("null.jpg")
                .build();
        UserProfileBatchResponse mockResponseDto = new UserProfileBatchResponse(List.of(profile1, profileNullId));

        mockWebServer.enqueue(
                new MockResponse()
                        .setBody(objectMapper.writeValueAsString(mockResponseDto))
                        .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));

        List<UUID> userIds = List.of(userId1, UUID.randomUUID()); // Requesting two IDs

        // Act
        Mono<Map<UUID, PostedByData>> resultMono = userProfileService.fetchUserProfilesBatch(userIds);

        // Assert
        StepVerifier.create(resultMono)
                .assertNext(profileMap -> {
                    assertNotNull(profileMap);
                    assertEquals(1, profileMap.size()); // Only profile1 should be present
                    assertEquals(profile1, profileMap.get(userId1));
                    assertNull(profileMap.get(null)); // Ensure the null ID one isn't mapped
                })
                .verifyComplete();
        // Verify request was made
        try {
            assertNotNull(mockWebServer.takeRequest(1, TimeUnit.SECONDS));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail("Test interrupted", e);
        }
    }

    @Test
    void fetchUserProfilesBatch_apiReturns500_returnsEmptyMap() {
        // Arrange
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));
        List<UUID> userIds = List.of(UUID.randomUUID());

        // Act
        Mono<Map<UUID, PostedByData>> resultMono = userProfileService.fetchUserProfilesBatch(userIds);

        // Assert
        StepVerifier.create(resultMono)
                .expectNext(Collections.emptyMap())
                .verifyComplete();
        // Verify request was made
        try {
            assertNotNull(mockWebServer.takeRequest(1, TimeUnit.SECONDS));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail("Test interrupted", e);
        }
    }

    @Test
    void fetchUserProfilesBatch_networkError_returnsEmptyMap() {
        // Arrange
        // Simulate network error by stopping the server temporarily (or using a
        // non-routable address)
        // For simplicity, we'll just test the onErrorResume path by providing an
        // invalid URL in a separate instance
        UserProfileService errorService = new UserProfileServiceImpl(WebClient.builder().build(),
                "http://invalid-url-that-does-not-exist");
        List<UUID> userIds = List.of(UUID.randomUUID());

        // Act
        Mono<Map<UUID, PostedByData>> resultMono = errorService.fetchUserProfilesBatch(userIds);

        // Assert
        StepVerifier.create(resultMono)
                .expectNext(Collections.emptyMap())
                .verifyComplete();
    }

    @Test
    void fetchUserProfilesBatch_apiReturnsMalformedJson_returnsEmptyMap() {
        // Arrange
        mockWebServer.enqueue(
                new MockResponse()
                        .setBody("{\"invalid json")
                        .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));
        List<UUID> userIds = List.of(UUID.randomUUID());

        // Act
        Mono<Map<UUID, PostedByData>> resultMono = userProfileService.fetchUserProfilesBatch(userIds);

        // Assert
        StepVerifier.create(resultMono)
                .expectNext(Collections.emptyMap())
                .verifyComplete();
        // Verify request was made
        try {
            assertNotNull(mockWebServer.takeRequest(1, TimeUnit.SECONDS));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail("Test interrupted", e);
        }
    }

    @Test
    void fetchUserProfilesBatch_apiReturnsDuplicateProfileIds_usesFirstOne() throws JsonProcessingException {
        // Arrange
        // Define two profiles with the SAME ID but different data
        PostedByData profile1_first = PostedByData.builder().userId(USER_ID_1).name("User One First")
                .profilePicture("pic1_first.jpg").build();
        PostedByData profile1_second = PostedByData.builder().userId(USER_ID_1).name("User One Second")
                .profilePicture("pic1_second.jpg").build();
        PostedByData profile2 = PostedByData.builder().userId(USER_ID_2).name("User Two").profilePicture("pic2.jpg")
                .build();

        // The response contains the duplicate ID and another unique ID
        UserProfileBatchResponse mockResponseDto = new UserProfileBatchResponse(
                List.of(profile1_first, profile1_second, profile2));

        mockWebServer.enqueue(
                new MockResponse()
                        .setBody(objectMapper.writeValueAsString(mockResponseDto))
                        .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));

        // Request distinct IDs
        List<UUID> userIds = List.of(USER_ID_1, USER_ID_2);

        // Act
        Mono<Map<UUID, PostedByData>> resultMono = userProfileService.fetchUserProfilesBatch(userIds);

        // Assert
        StepVerifier.create(resultMono)
                .assertNext(profileMap -> {
                    assertNotNull(profileMap);
                    assertEquals(2, profileMap.size()); // Should contain entries for USER_ID_1 and USER_ID_2

                    // Verify USER_ID_1 uses the data from the *first* occurrence in the response
                    // list
                    assertTrue(profileMap.containsKey(USER_ID_1));
                    assertEquals(profile1_first.getName(), profileMap.get(USER_ID_1).getName());
                    assertEquals(profile1_first.getProfilePicture(), profileMap.get(USER_ID_1).getProfilePicture());

                    // Verify USER_ID_2 is present and correct
                    assertTrue(profileMap.containsKey(USER_ID_2));
                    assertEquals(profile2.getName(), profileMap.get(USER_ID_2).getName());
                    assertEquals(profile2.getProfilePicture(), profileMap.get(USER_ID_2).getProfilePicture());
                })
                .verifyComplete();

        // Verify request was made
        try {
            var recordedRequest = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
            assertNotNull(recordedRequest);
            assertEquals("/api/profiles/batch", recordedRequest.getPath());
            UserProfileBatchRequest requestBody = objectMapper.readValue(recordedRequest.getBody().readUtf8(),
                    UserProfileBatchRequest.class);
            assertEquals(userIds, requestBody.getUserIds()); // Requested distinct IDs
        } catch (InterruptedException | IOException e) {
            fail("Failed to take request from mock server", e);
        }
    }
}
