package com.safetypin.post.dto;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserInfoDto {
    private UUID userId;
    private String name;
    private String profilePictureUrl; // Assuming the auth service provides a URL
}
