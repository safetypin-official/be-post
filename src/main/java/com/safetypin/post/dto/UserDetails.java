package com.safetypin.post.dto;

import io.jsonwebtoken.Claims;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class UserDetails {
    private String role;
    private boolean isVerified;
    private UUID userId;
    private String name;

    public static UserDetails fromClaims(Claims claims) {
        return new UserDetails(
                claims.get("role", String.class),
                claims.get("isVerified", Boolean.class),
                UUID.fromString(claims.get("userId", String.class)),
                claims.get("name", String.class)
        );
    }
}
