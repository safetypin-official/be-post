package com.safetypin.post.dto;

import java.util.UUID;

import io.jsonwebtoken.Claims;
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

    private String role;
    private boolean isVerified;
    private UUID userId;
    private String name;

    public static UserDetails fromClaims(Claims claims) {
        return new UserDetails(
                claims.get("role", String.class),
                claims.get("isVerified", Boolean.class),
                UUID.fromString(claims.get("userId", String.class)),
                claims.get("name", String.class));
    }

    public boolean isPremiumUser() {
        return "PREMIUM_USER".equals(role) || "MODERATOR".equals(role);
    }

    public int getTitleCharacterLimit() {
        return isPremiumUser() ? PREMIUM_USER_TITLE_LIMIT : REGISTERED_USER_TITLE_LIMIT;
    }

    public int getCaptionCharacterLimit() {
        return isPremiumUser() ? PREMIUM_USER_CAPTION_LIMIT : REGISTERED_USER_CAPTION_LIMIT;
    }
}
