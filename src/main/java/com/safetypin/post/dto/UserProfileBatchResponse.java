package com.safetypin.post.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO containing a list of user profiles from the batch endpoint.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileBatchResponse {
    private List<PostedByData> profiles;
}
