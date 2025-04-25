package com.safetypin.post.service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.safetypin.post.dto.PostedByData;
import com.safetypin.post.dto.UserProfileBatchRequest;
import com.safetypin.post.dto.UserProfileBatchResponse;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class UserProfileServiceImpl implements UserProfileService {

    private final WebClient webClient;

    @Value("${auth.service.url}")
    private String authServiceUrl;

    @Autowired
    public UserProfileServiceImpl(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    // Constructor for testing purposes
    public UserProfileServiceImpl(WebClient webClient, String authServiceUrl) {
        this.webClient = webClient;
        this.authServiceUrl = authServiceUrl;
    }

    @Override
    public Mono<Map<UUID, PostedByData>> fetchUserProfilesBatch(List<UUID> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            log.warn("fetchUserProfilesBatch called with null or empty userIds list.");
            return Mono.just(Collections.emptyMap());
        }

        // Remove duplicates
        List<UUID> distinctUserIds = userIds.stream().distinct().toList();

        String uri = authServiceUrl + "/api/profiles/batch";
        UserProfileBatchRequest request = new UserProfileBatchRequest(distinctUserIds);

        log.info("Fetching profiles for {} distinct users from {}", distinctUserIds.size(), uri);

        return webClient.post()
                .uri(uri)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(UserProfileBatchResponse.class)
                .map(response -> {
                    if (response == null || response.getProfiles() == null) {
                        log.warn("Received null response or null profiles list from batch user profile endpoint.");
                        return Collections.<UUID, PostedByData>emptyMap();
                    }
                    log.info("Successfully fetched {} profiles.", response.getProfiles().size());
                    // Convert list to map for easy lookup
                    return response.getProfiles().stream()
                            .filter(Objects::nonNull) // Ensure profile data is not null
                            .filter(profile -> profile.getUserId() != null) // Ensure profile has an ID
                            .collect(Collectors.toMap(
                                    PostedByData::getUserId,
                                    Function.identity(),
                                    (existing, replacement) -> { // Handle potential duplicates from API (though
                                                                 // unlikely with distinct IDs)
                                        log.warn(
                                                "Duplicate profile ID {} received from batch endpoint. Using the first one.",
                                                existing.getUserId());
                                        return existing;
                                    }));
                })
                .onErrorResume(error -> {
                    log.error("Error fetching user profiles from {}: {}", uri, error.getMessage(), error);
                    // Return an empty map in case of any error (network, parsing, etc.)
                    return Mono.just(Collections.emptyMap());
                });
    }
}
