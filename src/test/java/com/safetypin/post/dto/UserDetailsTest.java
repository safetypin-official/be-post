package com.safetypin.post.dto;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.RequiredTypeException;
import io.jsonwebtoken.impl.DefaultClaims;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UserDetailsTest {

    private static final UUID USER_ID_1 = UUID.randomUUID();
    private static final UUID USER_ID_2 = UUID.randomUUID();

    @Test
    void testGettersAndSetters() {
        // Arrange
        UserDetails userDetails = new UserDetails(
                "ADMIN", true, USER_ID_1, "John Doe");

        // Act & Assert
        assertEquals("ADMIN", userDetails.getRole());
        assertTrue(userDetails.isVerified());
        assertEquals(USER_ID_1, userDetails.getUserId());
        assertEquals("John Doe", userDetails.getName());

        // Act
        userDetails.setRole("USER");
        userDetails.setVerified(false);
        userDetails.setUserId(USER_ID_2);
        userDetails.setName("Jane Doe");

        // Assert
        assertEquals("USER", userDetails.getRole());
        assertFalse(userDetails.isVerified());
        assertEquals(USER_ID_2, userDetails.getUserId());
        assertEquals("Jane Doe", userDetails.getName());
    }

    @Test
    void testFromClaimsSuccess() {
        // Arrange
        UUID testUserId = UUID.randomUUID();
        Claims claims = new DefaultClaims();
        claims.put("role", "USER");
        claims.put("isVerified", true);
        claims.put("userId", testUserId.toString());
        claims.put("name", "John Doe");

        // Act
        UserDetails userDetails = UserDetails.fromClaims(claims);

        // Assert
        assertEquals("USER", userDetails.getRole());
        assertTrue(userDetails.isVerified());
        assertEquals(testUserId, userDetails.getUserId());
        assertEquals("John Doe", userDetails.getName());
    }

    @Test
    void testFromClaimsWithMissingValues() {
        // Arrange
        Claims claims = new DefaultClaims();
        claims.put("role", "USER");
        // Omit isVerified
        claims.put("userId", UUID.randomUUID().toString());
        claims.put("name", "John Doe");

        // Act & Assert
        assertThrows(NullPointerException.class, () -> UserDetails.fromClaims(claims));
    }

    @Test
    void testFromClaimsWithWrongTypes() {
        // Arrange
        Claims claims = new DefaultClaims();
        claims.put("role", "USER");
        claims.put("isVerified", "not-a-boolean");  // Wrong type
        claims.put("userId", UUID.randomUUID().toString());
        claims.put("name", "John Doe");

        // Act & Assert
        assertThrows(RequiredTypeException.class, () -> UserDetails.fromClaims(claims));
    }

    @Test
    void testFromClaimsWithInvalidUserId() {
        // Arrange
        Claims claims = new DefaultClaims();
        claims.put("role", "USER");
        claims.put("isVerified", true);
        claims.put("userId", "not-a-uuid");  // Invalid UUID
        claims.put("name", "John Doe");

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> UserDetails.fromClaims(claims));
    }

    @Test
    void testHashCode() {
        // Arrange
        UUID sharedId = UUID.randomUUID();
        UserDetails userDetails1 = new UserDetails(
                "ADMIN", true, sharedId, "John Doe");
        UserDetails userDetails2 = new UserDetails(
                "ADMIN", true, sharedId, "John Doe");
        UserDetails userDetails3 = new UserDetails(
                "USER", true, sharedId, "John Doe");

        // Act & Assert
        assertEquals(userDetails1, userDetails2);
        assertEquals(userDetails1.hashCode(), userDetails2.hashCode());
        assertNotEquals(userDetails1, userDetails3);
        assertNotEquals(userDetails1.hashCode(), userDetails3.hashCode());
    }
}