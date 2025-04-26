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

    @Enumerated(EnumType.STRING)
    private Role role;
    private boolean isVerified;
    private UUID userId;
    private String name;

    public static UserDetails fromClaims(Claims claims) {
        return new UserDetails(
                claims.get("role", Role.class),
                claims.get("isVerified", Boolean.class),
                UUID.fromString(claims.get("userId", String.class)),
                claims.get("name", String.class));
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
}
