package com.safetypin.post.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import com.safetypin.post.exception.InvalidCredentialsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.Key;
import java.util.Date;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    private JwtService jwtService;
    private Key key;
    private UUID testUserId;

    @BeforeEach
    void setUp() {
        String secretKey = "120c727764e959b6f15dfd537c43bde23a70c18f82318b4633009afbbdd8e250";
        key = Keys.hmacShaKeyFor(secretKey.getBytes());
        jwtService = new JwtService(secretKey);
        testUserId = UUID.randomUUID();
    }

    @Test
    void parseToken_ValidToken_ReturnsClaims() throws InvalidCredentialsException {
        // Given
        String token = createValidToken(testUserId);

        // When
        Claims claims = jwtService.parseToken(token);

        // Then
        assertNotNull(claims);
        assertEquals(testUserId.toString(), claims.getSubject());
    }

    @Test
    void parseToken_InvalidToken_ThrowsException() {
        // Given
        String invalidToken = "invalid.token.string";

        // When/Then
        assertThrows(InvalidCredentialsException.class, () -> jwtService.parseToken(invalidToken));
    }

    @Test
    void getUserIdFromJwtToken_ValidToken_ReturnsUserId() throws InvalidCredentialsException {
        // Given
        String token = createValidToken(testUserId);

        // When
        UUID userId = jwtService.getUserIdFromJwtToken(token);

        // Then
        assertEquals(testUserId, userId);
    }

    @Test
    void getUserIdFromJwtToken_ExpiredToken_ThrowsException() {
        // Given
        String expiredToken = createExpiredToken(testUserId);

        // When/Then
        InvalidCredentialsException exception = assertThrows(InvalidCredentialsException.class,
                () -> jwtService.getUserIdFromJwtToken(expiredToken));
        assertTrue(exception.getMessage().contains("Token expired"));
    }

    @Test
    void getUserIdFromJwtToken_InvalidToken_ThrowsException() {
        // Given
        String invalidToken = "invalid.token.string";

        // When/Then
        InvalidCredentialsException exception = assertThrows(InvalidCredentialsException.class,
                () -> jwtService.getUserIdFromJwtToken(invalidToken));
        assertEquals("JWT Access Token parsing failed", exception.getMessage());
    }

    @Test
    void getUserIdFromJwtToken_TokenWithoutExpiration_ThrowsException() {
        // Given
        String token = createTokenWithoutExpiration(testUserId);

        // When/Then
        InvalidCredentialsException exception = assertThrows(InvalidCredentialsException.class,
                () -> jwtService.getUserIdFromJwtToken(token));
        assertEquals("Token has no expired date", exception.getMessage());
    }

    @Test
    void getUserIdFromJwtToken_TokenWithInvalidSubject_ThrowsException() {
        // Given
        String token = createTokenWithInvalidSubject();

        // When/Then
        InvalidCredentialsException exception = assertThrows(InvalidCredentialsException.class,
                () -> jwtService.getUserIdFromJwtToken(token));
        assertEquals("Invalid subject", exception.getMessage());
    }

    @Test
    void getUserIdFromAuthorizationHeader_ValidHeader_ReturnsUserId() throws InvalidCredentialsException {
        // Given
        String token = createValidToken(testUserId);
        String authorizationHeader = "Bearer " + token;

        // When
        UUID userId = jwtService.getUserIdFromAuthorizationHeader(authorizationHeader);

        // Then
        assertEquals(testUserId, userId);
    }

    @Test
    void getUserIdFromAuthorizationHeader_InvalidHeader_ThrowsException() {
        // Given
        String invalidHeader = "InvalidHeader";

        // When/Then
        InvalidCredentialsException exception = assertThrows(InvalidCredentialsException.class,
                () -> jwtService.getUserIdFromAuthorizationHeader(invalidHeader));
        assertEquals("Authorization header is invalid", exception.getMessage());
    }

    @Test
    void getUserIdFromAuthorizationHeader_NullHeader_ThrowsException() {
        // When/Then
        InvalidCredentialsException exception = assertThrows(InvalidCredentialsException.class,
                () -> jwtService.getUserIdFromAuthorizationHeader(null));
        assertEquals("Authorization header is invalid", exception.getMessage());
    }

    @Test
    void getUserIdFromAuthorizationHeader_EmptyHeader_ThrowsException() {
        // Given
        String emptyHeader = "";

        // When/Then
        InvalidCredentialsException exception = assertThrows(InvalidCredentialsException.class,
                () -> jwtService.getUserIdFromAuthorizationHeader(emptyHeader));
        assertEquals("Authorization header is invalid", exception.getMessage());
    }

    @Test
    void getUserIdFromAuthorizationHeader_HeaderWithoutBearer_ThrowsException() {
        // Given
        String invalidHeader = "Token " + createValidToken(testUserId);

        // When/Then
        InvalidCredentialsException exception = assertThrows(InvalidCredentialsException.class,
                () -> jwtService.getUserIdFromAuthorizationHeader(invalidHeader));
        assertEquals("Authorization header is invalid", exception.getMessage());
    }

    private String createValidToken(UUID userId) {
        return Jwts.builder()
                .setSubject(userId.toString())
                .setExpiration(new Date(System.currentTimeMillis() + 1000L * 60 * 10)) // 10 minutes
                .signWith(key)
                .compact();

    }

    private String createExpiredToken(UUID userId) {
        return Jwts.builder()
                .setSubject(userId.toString())
                .setExpiration(new Date(System.currentTimeMillis() - 1000L * 60 * 10)) // 10 minutes ago
                .signWith(key)
                .compact();


    }

    private String createTokenWithoutExpiration(UUID userId) {
        return Jwts.builder()
                .setSubject(userId.toString())
                .signWith(key)
                .compact();
    }

    private String createTokenWithInvalidSubject() {
        return Jwts.builder()
                .setSubject("invalid-uuid")
                .setExpiration(new Date(System.currentTimeMillis() + 1000L * 60 * 10))
                .signWith(key)
                .compact();
    }
} 