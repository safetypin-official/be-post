package com.safetypin.post.dto;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.impl.DefaultClaims;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserDetailsTest {

    @Test
    void testGettersAndSetters() {
        // Arrange
        UserDetails userDetails = new UserDetails("ADMIN", true, "user123", "John Doe");

        // Act & Assert
        assertEquals("ADMIN", userDetails.getRole());
        assertTrue(userDetails.isVerified());
        assertEquals("user123", userDetails.getUserId());
        assertEquals("John Doe", userDetails.getName());

        // Act
        userDetails.setRole("USER");
        userDetails.setVerified(false);
        userDetails.setUserId("user456");
        userDetails.setName("Jane Doe");

        // Assert
        assertEquals("USER", userDetails.getRole());
        assertFalse(userDetails.isVerified());
        assertEquals("user456", userDetails.getUserId());
        assertEquals("Jane Doe", userDetails.getName());
    }

    @Test
    void testFromClaimsSuccess() {
        // Arrange
        Claims claims = new DefaultClaims();
        claims.put("role", "USER");
        claims.put("isVerified", true);
        claims.put("userId", "user123");
        claims.put("name", "John Doe");

        // Act
        UserDetails userDetails = UserDetails.fromClaims(claims);

        // Assert
        assertEquals("USER", userDetails.getRole());
        assertTrue(userDetails.isVerified());
        assertEquals("user123", userDetails.getUserId());
        assertEquals("John Doe", userDetails.getName());
    }

    @Test
    void testFromClaimsWithMissingValues() {
        // Arrange
        Claims claims = new DefaultClaims();
        claims.put("role", "USER");
        // Omit isVerified
        claims.put("userId", "user123");
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
        claims.put("userId", "user123");
        claims.put("name", "John Doe");

        // Act & Assert
        assertThrows(ClassCastException.class, () -> UserDetails.fromClaims(claims));
    }

    @Test
    void testHashCode() {
        // Arrange
        UserDetails userDetails1 = new UserDetails(
                "ADMIN", true, "user123", "John Doe");
        UserDetails userDetails2 = new UserDetails(
                "ADMIN", true, "user123", "John Doe");
        UserDetails userDetails3 = new UserDetails(
                "USER", true, "user123", "John Doe");

        // Act & Assert
        assertEquals(userDetails1, userDetails2);
        assertEquals(userDetails1.hashCode(), userDetails2.hashCode());
        assertNotEquals(userDetails1, userDetails3);
        assertNotEquals(userDetails1.hashCode(), userDetails3.hashCode());
    }
}