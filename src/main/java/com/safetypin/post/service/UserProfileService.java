package com.safetypin.post.service;

import com.safetypin.post.dto.PostedByData;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service interface for interacting with the User Profile microservice.
 */
public interface UserProfileService {

    /**
     * Fetches user profile data in batch for the given user IDs.
     *
     * @param userIds A list of user IDs to fetch profiles for.
     * @return A Mono emitting a map where the key is the user ID and the value is
     * the corresponding PostedByData.
     * Returns an empty map if the input list is empty or null.
     */
    Mono<Map<UUID, PostedByData>> fetchUserProfilesBatch(List<UUID> userIds);
}
