package com.safetypin.post.integration;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import com.safetypin.post.config.IntegrationTestSecurityConfig;
import com.safetypin.post.model.Role;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(IntegrationTestSecurityConfig.class)
class AdminDeleteIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    // We don't need to autowire AdminService as we're not mocking it
    // for integration tests (the test config already handles this)

    @Value("${jwt.secret:testSecretKeyForDevelopmentOnly}")
    private String jwtSecretString;

    private UUID targetUserId;
    private UUID moderatorId;
    private UUID regularUserId;
    private String moderatorToken;
    private String regularUserToken;

    @BeforeEach
    void setup() {
        targetUserId = UUID.randomUUID();
        moderatorId = UUID.randomUUID();
        regularUserId = UUID.randomUUID();

        // Create actual JWT tokens with valid values
        moderatorToken = createJwtToken(moderatorId, Role.MODERATOR, "Moderator User");
        regularUserToken = createJwtToken(regularUserId, Role.REGISTERED_USER, "Regular User");
    }

    private String createJwtToken(UUID userId, Role role, String name) {
        // Create signing key from the JWT secret
        Key key = Keys.hmacShaKeyFor(jwtSecretString.getBytes(StandardCharsets.UTF_8));

        // Build claims
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role.name());
        claims.put("isVerified", true);
        claims.put("userId", userId.toString());
        claims.put("name", name);

        // Create token with proper signature and expiration time
        return Jwts.builder()
                .setClaims(claims) // Set all claims at once
                .setSubject(userId.toString())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 3600000)) // 1 hour in future
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    // @Test
    // void deleteUserContent_WithValidModeratorToken_ReturnsAccepted() throws
    // Exception {
    // // Act & Assert
    // mockMvc.perform(MockMvcRequestBuilders
    // .delete("/posts/admin/delete/" + targetUserId)
    // .header(HttpHeaders.AUTHORIZATION, "Bearer " + moderatorToken)
    // .contentType(MediaType.APPLICATION_JSON))
    // .andExpect(MockMvcResultMatchers.status().isAccepted())
    // .andExpect(MockMvcResultMatchers.jsonPath("$.message").exists())
    // .andExpect(MockMvcResultMatchers.jsonPath("$.userId").value(targetUserId.toString()));
    // }

    @Test
    void deleteUserContent_WithRegularUserToken_ReturnsForbidden() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders
                .delete("/posts/admin/delete/" + targetUserId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + regularUserToken)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isForbidden());
    }

    @Test
    void deleteUserContent_WithNoToken_ReturnsForbidden() throws Exception {
        // Without a token, Spring Security returns 403 Forbidden for admin endpoints
        mockMvc.perform(MockMvcRequestBuilders
                .delete("/posts/admin/delete/" + targetUserId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isForbidden());
    }

    @Test
    void deleteUserContent_WithInvalidUserId_ReturnsBadRequest() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders
                .delete("/posts/admin/delete/not-a-uuid")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + moderatorToken)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isBadRequest());
    }
}
