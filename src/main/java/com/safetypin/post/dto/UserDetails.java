package com.safetypin.post.dto;

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
        String roleStr = claims.get("role", String.class);
        Role role = (roleStr != null) ? Role.valueOf(roleStr) : Role.REGISTERED_USER;

        Boolean isVerified = claims.get("isVerified", Boolean.class);
        if (isVerified == null) {
            isVerified = false;
        }
        String userIdStr = claims.get("userId", String.class);
        UUID userId;
        try {
            userId = (userIdStr != null) ? UUID.fromString(userIdStr) : UUID.randomUUID();
        } catch (IllegalArgumentException e) {
            // If the userId is not a valid UUID format, throw an exception
            throw new IllegalArgumentException("Invalid UUID format: " + userIdStr);
        }

        String name = claims.get("name", String.class);
        if (name == null) {
            name = "Anonymous User";
        }

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
