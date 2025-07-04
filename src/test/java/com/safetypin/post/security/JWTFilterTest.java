package com.safetypin.post.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import com.safetypin.post.dto.UserDetails;
import com.safetypin.post.model.Role;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

class JWTFilterTest {

    private final UUID testUserId = UUID.randomUUID();
    private final String testToken = "valid.test.token";
    @Mock
    private JWTUtil jwtUtil;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private FilterChain filterChain;
    @Mock
    private Claims claims;
    @InjectMocks
    private JWTFilter jwtFilter;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilterInternal_ValidToken_Success() throws ServletException, IOException {
        // Arrange
        when(request.getHeader("Authorization")).thenReturn("Bearer " + testToken);
        when(jwtUtil.verifyAndGetClaims(testToken)).thenReturn(claims);

        when(claims.get("role", String.class)).thenReturn(Role.REGISTERED_USER.name());
        when(claims.get("isVerified", Boolean.class)).thenReturn(true);
        when(claims.get("userId", String.class)).thenReturn(testUserId.toString());
        when(claims.get("name", String.class)).thenReturn("John Doe");

        // Act
        jwtFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(filterChain).doFilter(request, response);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        assertEquals(Role.REGISTERED_USER, userDetails.getRole());
        assertTrue(userDetails.isVerified());
        assertEquals(testUserId, userDetails.getUserId());
        assertEquals("John Doe", userDetails.getName());

        assertTrue(authentication.getAuthorities().contains(new SimpleGrantedAuthority("REGISTERED_USER")));
    }

    @Test
    void doFilterInternal_MissingAuthHeader_NotAuthorized() throws ServletException, IOException {
        // Arrange
        when(request.getHeader("Authorization")).thenReturn(null);

        // Act
        jwtFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void doFilterInternal_EmptyAuthHeader_Unauthorized() throws ServletException, IOException {
        // Arrange
        when(request.getHeader("Authorization")).thenReturn("");

        // Act
        jwtFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(filterChain).doFilter(request, response);
        // No error response sent since we continue filter chain
        verify(response, never()).sendError(anyInt(), anyString());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void doFilterInternal_InvalidAuthHeaderFormat_Unauthorized() throws ServletException, IOException {
        // Arrange
        when(request.getHeader("Authorization")).thenReturn("InvalidFormat " + testToken);

        // Act
        jwtFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(filterChain).doFilter(request, response);
        // No error response sent since we continue filter chain
        verify(response, never()).sendError(anyInt(), anyString());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void doFilterInternal_InvalidToken_Unauthorized() throws ServletException, IOException {
        // Arrange
        when(request.getHeader("Authorization")).thenReturn("Bearer " + testToken);
        when(jwtUtil.verifyAndGetClaims(testToken))
                .thenThrow(new com.safetypin.post.exception.InvalidCredentialsException(
                        "Invalid JWT token: Test exception"));

        // Act
        jwtFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(filterChain).doFilter(request, response);
        // No error response sent since we continue filter chain
        verify(response, never()).sendError(anyInt(), anyString());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void doFilterInternal_InvalidClaimsData_Unauthorized() throws ServletException, IOException {
        // Arrange
        when(request.getHeader("Authorization")).thenReturn("Bearer " + testToken);
        when(jwtUtil.verifyAndGetClaims(testToken)).thenReturn(claims);

        // Missing required claim
        when(claims.get("role", String.class)).thenReturn(Role.MODERATOR.name());
        when(claims.get("isVerified", Boolean.class)).thenReturn(null); // This will be handled by UserDetails
                                                                        // fromClaims
        when(claims.get("userId", String.class)).thenReturn(testUserId.toString());
        when(claims.get("name", String.class)).thenReturn("Moderator User");

        // Act
        jwtFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(filterChain).doFilter(request, response);
        // No error response sent since we continue filter chain
        verify(response, never()).sendError(eq(HttpServletResponse.SC_UNAUTHORIZED), eq("Authentication failed"));

        // Authentication should succeed with default value for isVerified
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        assertEquals(Role.MODERATOR, userDetails.getRole());
        assertFalse(userDetails.isVerified()); // Default value should be false
    }

    @Test
    void doFilterInternal_InvalidUUID_Unauthorized() throws ServletException, IOException {
        // Arrange
        when(request.getHeader("Authorization")).thenReturn("Bearer " + testToken);
        when(jwtUtil.verifyAndGetClaims(testToken)).thenReturn(claims);

        // Invalid UUID format
        when(claims.get("role", String.class)).thenReturn(Role.REGISTERED_USER.name());
        when(claims.get("isVerified", Boolean.class)).thenReturn(true);
        when(claims.get("userId", String.class)).thenReturn("not-a-valid-uuid");
        when(claims.get("name", String.class)).thenReturn("John Doe");

        // Act
        jwtFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(filterChain).doFilter(request, response); // Filter chain continues
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void doFilterInternal_AdminRole_Success() throws ServletException, IOException {
        // Arrange
        when(request.getHeader("Authorization")).thenReturn("Bearer " + testToken);
        when(jwtUtil.verifyAndGetClaims(testToken)).thenReturn(claims);

        when(claims.get("role", String.class)).thenReturn(Role.MODERATOR.name());
        when(claims.get("isVerified", Boolean.class)).thenReturn(true);
        when(claims.get("userId", String.class)).thenReturn(testUserId.toString());
        when(claims.get("name", String.class)).thenReturn("Moderator User");

        // Act
        jwtFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(filterChain).doFilter(request, response);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        assertEquals(Role.MODERATOR, userDetails.getRole());
        assertTrue(authentication.getAuthorities().contains(new SimpleGrantedAuthority("MODERATOR")));
    }
}