package com.safetypin.post.dto;

import java.util.Optional;
import java.util.UUID;

import com.safetypin.post.model.Role;

import io.jsonwebtoken.Claims;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserDetails {

    // Character limit constants for different user roles

    public static final int REGISTERED_USER_TITLE_LIMIT = 70;
    public static final int PREMIUM_USER_TITLE_LIMIT = 140;

    public static final int REGISTERED_USER_CAPTION_LIMIT = 200;
    public static final int PREMIUM_USER_CAPTION_LIMIT = 800;

    public static final int REGISTERED_USER_POST_PER_DAY_LIMIT = 3;
    public static final int PREMIUM_USER_POST_PER_DAY_LIMIT = 10;

    @Enumerated(EnumType.STRING)
    private Role role;
    private boolean isVerified;
    private UUID userId;
    private String name;

    public static UserDetails fromClaims(Claims claims) {
        // Parse role with default value if null
        Role role = Optional.ofNullable(claims.get("role", String.class))
                .map(Role::valueOf)
                .orElse(Role.REGISTERED_USER);

        // Parse isVerified with default value if null
        boolean isVerified = Optional.ofNullable(claims.get("isVerified", Boolean.class))
                .orElse(false);

        // Get userId, which is required
        String userIdStr = claims.get("userId", String.class);
        if (userIdStr == null) {
            throw new IllegalArgumentException("User ID is missing from token claims");
        }

        // Parse userId as UUID
        UUID userId;
        try {
            userId = UUID.fromString(userIdStr);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid UUID format: " + userIdStr);
        }

        // Get name with default value if null
        String name = Optional.ofNullable(claims.get("name", String.class))
                .orElse("Anonymous User");

        return new UserDetails(role, isVerified, userId, name);
    }

    public boolean isPremiumUser() {
        return role != null && (role == Role.PREMIUM_USER || role == Role.MODERATOR);
    }

    public int getTitleCharacterLimit() {
        return isPremiumUser() ? PREMIUM_USER_TITLE_LIMIT : REGISTERED_USER_TITLE_LIMIT;
    }

    public int getCaptionCharacterLimit() {
        return isPremiumUser() ? PREMIUM_USER_CAPTION_LIMIT : REGISTERED_USER_CAPTION_LIMIT;
    }

    public int getPostPerDayLimit() {
        return isPremiumUser() ? PREMIUM_USER_POST_PER_DAY_LIMIT : REGISTERED_USER_POST_PER_DAY_LIMIT;
    }
}
