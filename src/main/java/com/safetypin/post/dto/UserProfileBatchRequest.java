package com.safetypin.post.dto;

import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for batch user profile fetching.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileBatchRequest {
    private List<UUID> userIds;
}
