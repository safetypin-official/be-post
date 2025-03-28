package com.safetypin.post.security;

import com.safetypin.post.exception.InvalidCredentialsException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.Key;
import java.util.Date;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JWTUtilTest {

    private JWTUtil jwtUtil;
    private Key key;
    private final UUID userId = UUID.randomUUID();
    private final String userIdString = userId.toString();

    @BeforeEach
    void setUp() {
        // Arrange
        String secretKey = "thisisasecretkeyforunittestinglongenoughtosignajwttoken";
        jwtUtil = new JWTUtil(secretKey);
        key = Keys.hmacShaKeyFor(secretKey.getBytes());
    }

    @Test
    void testVerifyAndGetClaims_ValidToken() {
        // Arrange
        long now = System.currentTimeMillis();
        long expirationTime = now + 3600000; // 1 hour from now

        String validToken = Jwts.builder()
                .setSubject("user123")
                .claim("role", "USER")
                .claim("name", "John Doe")
                .claim("isVerified", true)
                .claim("userId", userIdString)
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(expirationTime))
                .signWith(key)
                .compact();

        // Act
        Claims claims = jwtUtil.verifyAndGetClaims(validToken);

        // Assert
        assertNotNull(claims);
        assertEquals("user123", claims.getSubject());
        assertEquals("USER", claims.get("role"));
        assertEquals("John Doe", claims.get("name"));
        assertTrue((Boolean) claims.get("isVerified"));
        assertEquals(userIdString, claims.get("userId"));
    }

    @Test
    void testVerifyAndGetClaims_ExpiredToken() {
        // Arrange
        long now = System.currentTimeMillis();
        long expirationTime = now - 3600000; // 1 hour ago (expired)

        String expiredToken = Jwts.builder()
                .setSubject("user123")
                .setIssuedAt(new Date(now - 7200000)) // 2 hours ago
                .setExpiration(new Date(expirationTime))
                .signWith(key)
                .compact();

        // Act & Assert
        assertThrows(InvalidCredentialsException.class,
                () -> jwtUtil.verifyAndGetClaims(expiredToken));
    }

    @Test
    void testVerifyAndGetClaims_InvalidSignature() {
        // Arrange
        String wrongKey = "anothersecretkeythatisalsoquitelongbutdifferent";
        Key wrongSigningKey = Keys.hmacShaKeyFor(wrongKey.getBytes());

        String invalidToken = Jwts.builder()
                .setSubject("user123")
                .setExpiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(wrongSigningKey)
                .compact();

        // Act & Assert
        InvalidCredentialsException exception = assertThrows(InvalidCredentialsException.class,
                () -> jwtUtil.verifyAndGetClaims(invalidToken));
        assertTrue(exception.getMessage().contains("Invalid JWT token"));
    }

    @Test
    void testVerifyAndGetClaims_MalformedToken() {
        // Arrange
        String malformedToken = "this.is.not.a.valid.jwt.token";

        // Act & Assert
        InvalidCredentialsException exception = assertThrows(InvalidCredentialsException.class,
                () -> jwtUtil.verifyAndGetClaims(malformedToken));
        assertTrue(exception.getMessage().contains("Invalid JWT token"));
    }

    @Test
    void testVerifyAndGetClaims_EmptyToken() {
        // Arrange
        String emptyToken = "";

        // Act & Assert
        InvalidCredentialsException exception = assertThrows(InvalidCredentialsException.class,
                () -> jwtUtil.verifyAndGetClaims(emptyToken));
        assertTrue(exception.getMessage().contains("Invalid JWT token"));
    }

    @Test
    void testVerifyAndGetClaims_NullToken() {
        // Arrange
        String nullToken = null;

        // Act & Assert
        InvalidCredentialsException exception = assertThrows(InvalidCredentialsException.class,
                () -> jwtUtil.verifyAndGetClaims(nullToken));
        assertTrue(exception.getMessage().contains("Invalid JWT token"));
    }

    @Test
    void testVerifyAndGetClaims_TokenWithoutExpiration() {
        // Arrange
        String tokenWithoutExpiration = Jwts.builder()
                .setSubject("user123")
                .claim("role", "USER")
                .signWith(key)
                .compact();

        // Act & Assert
        assertThrows(InvalidCredentialsException.class,
                () -> jwtUtil.verifyAndGetClaims(tokenWithoutExpiration));
    }

    @Test
    void testVerifyAndGetClaims_TokenWithAllUserDetails() {
        // Arrange
        UUID userId2 = UUID.randomUUID();
        long now = System.currentTimeMillis();
        long expirationTime = now + 3600000; // 1 hour from now

        String token = Jwts.builder()
                .setSubject("user456")
                .claim("role", "ADMIN")
                .claim("name", "Jane Smith")
                .claim("isVerified", false)
                .claim("userId", userId2.toString())
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(expirationTime))
                .signWith(key)
                .compact();

        // Act
        Claims claims = jwtUtil.verifyAndGetClaims(token);

        // Assert
        assertNotNull(claims);
        assertEquals("user456", claims.getSubject());
        assertEquals("ADMIN", claims.get("role"));
        assertEquals("Jane Smith", claims.get("name"));
        assertFalse((Boolean) claims.get("isVerified"));
        assertEquals(userId2.toString(), claims.get("userId"));
    }
}