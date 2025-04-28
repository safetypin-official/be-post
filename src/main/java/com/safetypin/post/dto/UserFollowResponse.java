package com.safetypin.post.dto;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserFollowResponse {
    private UUID userId;
    private String name;
    private String profilePicture;
    private boolean following;
}