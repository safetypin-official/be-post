package com.safetypin.post.dto;

import com.safetypin.post.model.Role;
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
                Role.PREMIUM_USER, true, USER_ID_1, "John Doe");

        // Act & Assert
        assertEquals(Role.PREMIUM_USER, userDetails.getRole());
        assertTrue(userDetails.isVerified());
        assertEquals(USER_ID_1, userDetails.getUserId());
        assertEquals("John Doe", userDetails.getName());

        // Act
        userDetails.setRole(Role.REGISTERED_USER);
        userDetails.setVerified(false);
        userDetails.setUserId(USER_ID_2);
        userDetails.setName("Jane Doe");

        // Assert
        assertEquals(Role.REGISTERED_USER, userDetails.getRole());
        assertFalse(userDetails.isVerified());
        assertEquals(USER_ID_2, userDetails.getUserId());
        assertEquals("Jane Doe", userDetails.getName());
    }

    @Test
    void testFromClaimsSuccess() {
        // Arrange
        UUID testUserId = UUID.randomUUID();
        Claims claims = new DefaultClaims();
        claims.put("role", Role.REGISTERED_USER.name());
        claims.put("isVerified", true);
        claims.put("userId", testUserId.toString());
        claims.put("name", "John Doe");

        // Act
        UserDetails userDetails = UserDetails.fromClaims(claims);

        // Assert
        assertEquals(Role.REGISTERED_USER, userDetails.getRole());
        assertTrue(userDetails.isVerified());
        assertEquals(testUserId, userDetails.getUserId());
        assertEquals("John Doe", userDetails.getName());
    }

    @Test
    void testFromClaimsWithMissingValues() {
        // Arrange
        Claims claims = new DefaultClaims();
        claims.put("role", Role.REGISTERED_USER.name());
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
        claims.put("role", Role.REGISTERED_USER.name());
        claims.put("isVerified", "not-a-boolean"); // Wrong type
        claims.put("userId", UUID.randomUUID().toString());
        claims.put("name", "John Doe");

        // Act & Assert
        assertThrows(RequiredTypeException.class, () -> UserDetails.fromClaims(claims));
    }

    @Test
    void testFromClaimsWithInvalidUserId() {
        // Arrange
        Claims claims = new DefaultClaims();
        claims.put("role", Role.REGISTERED_USER.name());
        claims.put("isVerified", true);
        claims.put("userId", "not-a-uuid"); // Invalid UUID
        claims.put("name", "John Doe");

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> UserDetails.fromClaims(claims));
    }

    @Test
    void testHashCode() {
        // Arrange
        UUID sharedId = UUID.randomUUID();
        UserDetails userDetails1 = new UserDetails(
                Role.PREMIUM_USER, true, sharedId, "John Doe");
        UserDetails userDetails2 = new UserDetails(
                Role.PREMIUM_USER, true, sharedId, "John Doe");
        UserDetails userDetails3 = new UserDetails(
                Role.REGISTERED_USER, true, sharedId, "John Doe");

        // Act & Assert
        assertEquals(userDetails1, userDetails2);
        assertEquals(userDetails1.hashCode(), userDetails2.hashCode());
        assertNotEquals(userDetails1, userDetails3);
        assertNotEquals(userDetails1.hashCode(), userDetails3.hashCode());
    }

    // Tests for isPremiumUser() method
    @Test
    void testIsPremiumUser_WithPremiumUserRole() {
        // Arrange
        UserDetails userDetails = new UserDetails(
                Role.PREMIUM_USER, true, USER_ID_1, "Premium User");

        // Act & Assert
        assertTrue(userDetails.isPremiumUser());
    }

    @Test
    void testIsPremiumUser_WithModeratorRole() {
        // Arrange
        UserDetails userDetails = new UserDetails(
                Role.MODERATOR, true, USER_ID_1, "Moderator User");

        // Act & Assert
        assertTrue(userDetails.isPremiumUser());
    }

    @Test
    void testIsPremiumUser_WithRegularUserRole() {
        // Arrange
        UserDetails userDetails = new UserDetails(
                Role.REGISTERED_USER, true, USER_ID_1, "Regular User");

        // Act & Assert
        assertFalse(userDetails.isPremiumUser());
    }

    @Test
    void testIsPremiumUser_WithNullRole() {
        // Arrange
        UserDetails userDetails = new UserDetails(
                null, true, USER_ID_1, "User with null role");

        // Act & Assert
        assertFalse(userDetails.isPremiumUser());
    }

    // Tests for Title Character Limits

    // For REGISTERED_USER (Free Users)
    @Test
    void testGetTitleCharacterLimit_ForRegularUser() {
        // Arrange
        UserDetails userDetails = new UserDetails(
                Role.REGISTERED_USER, true, USER_ID_1, "Regular User");

        // Act
        int limit = userDetails.getTitleCharacterLimit();

        // Assert
        assertEquals(UserDetails.REGISTERED_USER_TITLE_LIMIT, limit);
        assertEquals(70, limit);
    }

    // For PREMIUM_USER
    @Test
    void testGetTitleCharacterLimit_ForPremiumUser() {
        // Arrange
        UserDetails userDetails = new UserDetails(
                Role.PREMIUM_USER, true, USER_ID_1, "Premium User");

        // Act
        int limit = userDetails.getTitleCharacterLimit();

        // Assert
        assertEquals(UserDetails.PREMIUM_USER_TITLE_LIMIT, limit);
        assertEquals(140, limit);
    }

    @Test
    void testGetTitleCharacterLimit_ForModerator() {
        // Arrange
        UserDetails userDetails = new UserDetails(
                Role.MODERATOR, true, USER_ID_1, "Moderator User");

        // Act
        int limit = userDetails.getTitleCharacterLimit();

        // Assert
        assertEquals(UserDetails.PREMIUM_USER_TITLE_LIMIT, limit);
        assertEquals(140, limit);
    }

    // Tests for Caption Character Limits

    // For REGISTERED_USER (Free Users)
    @Test
    void testGetCaptionCharacterLimit_ForRegularUser() {
        // Arrange
        UserDetails userDetails = new UserDetails(
                Role.REGISTERED_USER, true, USER_ID_1, "Regular User");

        // Act
        int limit = userDetails.getCaptionCharacterLimit();

        // Assert
        assertEquals(UserDetails.REGISTERED_USER_CAPTION_LIMIT, limit);
        assertEquals(200, limit);
    }

    // For PREMIUM_USER
    @Test
    void testGetCaptionCharacterLimit_ForPremiumUser() {
        // Arrange
        UserDetails userDetails = new UserDetails(
                Role.PREMIUM_USER, true, USER_ID_1, "Premium User");

        // Act
        int limit = userDetails.getCaptionCharacterLimit();

        // Assert
        assertEquals(UserDetails.PREMIUM_USER_CAPTION_LIMIT, limit);
        assertEquals(800, limit);
    }

    @Test
    void testGetCaptionCharacterLimit_ForModerator() {
        // Arrange
        UserDetails userDetails = new UserDetails(
                Role.MODERATOR, true, USER_ID_1, "Moderator User");

        // Act
        int limit = userDetails.getCaptionCharacterLimit();

        // Assert
        assertEquals(UserDetails.PREMIUM_USER_CAPTION_LIMIT, limit);
        assertEquals(800, limit);
    }

    // Boundary test cases for character limits
    @Test
    void testConstantValues_EnsureCorrectLimits() {
        // Assert - verify the constant values are set correctly
        assertEquals(70, UserDetails.REGISTERED_USER_TITLE_LIMIT);
        assertEquals(140, UserDetails.PREMIUM_USER_TITLE_LIMIT);
        assertEquals(200, UserDetails.REGISTERED_USER_CAPTION_LIMIT);
        assertEquals(800, UserDetails.PREMIUM_USER_CAPTION_LIMIT);
    }

    // Additional tests for creating properly bounded posts
    @Test
    void testTitleBoundaryConditions() {
        // Test exactly at the registered user limit
        UserDetails registeredUser = new UserDetails(Role.REGISTERED_USER, true, USER_ID_1, "Regular User");
        assertEquals(70, registeredUser.getTitleCharacterLimit());

        // Test exactly at the premium user limit
        UserDetails premiumUser = new UserDetails(Role.PREMIUM_USER, true, USER_ID_1, "Premium User");
        assertEquals(140, premiumUser.getTitleCharacterLimit());

        // Test difference between limits
        assertEquals(70, premiumUser.getTitleCharacterLimit() - registeredUser.getTitleCharacterLimit());
    }

    @Test
    void testCaptionBoundaryConditions() {
        // Test exactly at the registered user limit
        UserDetails registeredUser = new UserDetails(Role.REGISTERED_USER, true, USER_ID_1, "Regular User");
        assertEquals(200, registeredUser.getCaptionCharacterLimit());

        // Test exactly at the premium user limit
        UserDetails premiumUser = new UserDetails(Role.PREMIUM_USER, true, USER_ID_1, "Premium User");
        assertEquals(800, premiumUser.getCaptionCharacterLimit());

        // Test difference between limits
        assertEquals(600, premiumUser.getCaptionCharacterLimit() - registeredUser.getCaptionCharacterLimit());
    }

    @Test
    void testIsPremiumUserMethodInfluencesLimits() {
        // Create user details with a role that will return false for isPremiumUser
        UserDetails regularUser = new UserDetails(Role.REGISTERED_USER, true, USER_ID_1, "Regular User");
        assertFalse(regularUser.isPremiumUser());
        assertEquals(UserDetails.REGISTERED_USER_TITLE_LIMIT, regularUser.getTitleCharacterLimit());
        assertEquals(UserDetails.REGISTERED_USER_CAPTION_LIMIT, regularUser.getCaptionCharacterLimit());

        // Create user details with a role that will return true for isPremiumUser
        UserDetails premiumUser = new UserDetails(Role.PREMIUM_USER, true, USER_ID_1, "Premium User");
        assertTrue(premiumUser.isPremiumUser());
        assertEquals(UserDetails.PREMIUM_USER_TITLE_LIMIT, premiumUser.getTitleCharacterLimit());
        assertEquals(UserDetails.PREMIUM_USER_CAPTION_LIMIT, premiumUser.getCaptionCharacterLimit());

        // Verify that the moderator also gets premium limits
        UserDetails moderator = new UserDetails(Role.MODERATOR, true, USER_ID_1, "Moderator User");
        assertTrue(moderator.isPremiumUser());
        assertEquals(UserDetails.PREMIUM_USER_TITLE_LIMIT, moderator.getTitleCharacterLimit());
        assertEquals(UserDetails.PREMIUM_USER_CAPTION_LIMIT, moderator.getCaptionCharacterLimit());
    }

    @Test
    void testCustomRoleCharacterLimits() {
        // A role that doesn't match any predefined premium roles should get registered
        // user limits
        UserDetails customRoleUser = new UserDetails(null, true, USER_ID_1, "Custom Role User");
        assertFalse(customRoleUser.isPremiumUser());
        assertEquals(UserDetails.REGISTERED_USER_TITLE_LIMIT, customRoleUser.getTitleCharacterLimit());
        assertEquals(UserDetails.REGISTERED_USER_CAPTION_LIMIT, customRoleUser.getCaptionCharacterLimit());
    }
}