package com.safetypin.post.security;

import com.safetypin.post.dto.UserDetails;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.io.IOException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JWTFilterTest {

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

    private final UUID testUserId = UUID.randomUUID();
    private final String testToken = "valid.test.token";

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

        when(claims.get("role", String.class)).thenReturn("USER");
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
        assertEquals("USER", userDetails.getRole());
        assertTrue(userDetails.isVerified());
        assertEquals(testUserId, userDetails.getUserId());
        assertEquals("John Doe", userDetails.getName());

        assertTrue(authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_USER")));
    }



    @Test
    void doFilterInternal_MissingAuthHeader_Unauthorized() throws ServletException, IOException {
        // Arrange
        when(request.getHeader("Authorization")).thenReturn(null);

        // Act
        jwtFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(response).sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication token required");
        verify(filterChain, never()).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void doFilterInternal_EmptyAuthHeader_Unauthorized() throws ServletException, IOException {
        // Arrange
        when(request.getHeader("Authorization")).thenReturn("");

        // Act
        jwtFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(response).sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication token required");
        verify(filterChain, never()).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void doFilterInternal_InvalidAuthHeaderFormat_Unauthorized() throws ServletException, IOException {
        // Arrange
        when(request.getHeader("Authorization")).thenReturn("InvalidFormat " + testToken);

        // Act
        jwtFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(response).sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication token required");
        verify(filterChain, never()).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void doFilterInternal_InvalidToken_Unauthorized() throws ServletException, IOException {
        // Arrange
        when(request.getHeader("Authorization")).thenReturn("Bearer " + testToken);
        when(jwtUtil.verifyAndGetClaims(testToken))
                .thenThrow(new com.safetypin.post.exception.InvalidCredentialsException("Invalid JWT token: Test exception"));

        // Act
        jwtFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(response).sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid JWT token");
        verify(filterChain, never()).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void doFilterInternal_InvalidClaimsData_Unauthorized() throws ServletException, IOException {
        // Arrange
        when(request.getHeader("Authorization")).thenReturn("Bearer " + testToken);
        when(jwtUtil.verifyAndGetClaims(testToken)).thenReturn(claims);

        // Missing required claim
        when(claims.get("role", String.class)).thenReturn("ADMIN");
        when(claims.get("isVerified", Boolean.class)).thenReturn(null); // This will cause NullPointerException
        when(claims.get("userId", String.class)).thenReturn(testUserId.toString());
        when(claims.get("name", String.class)).thenReturn("Admin User");

        // Act
        jwtFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(response).sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication failed");
        verify(filterChain, never()).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void doFilterInternal_InvalidUUID_Unauthorized() throws ServletException, IOException {
        // Arrange
        when(request.getHeader("Authorization")).thenReturn("Bearer " + testToken);
        when(jwtUtil.verifyAndGetClaims(testToken)).thenReturn(claims);

        // Invalid UUID format
        when(claims.get("role", String.class)).thenReturn("USER");
        when(claims.get("isVerified", Boolean.class)).thenReturn(true);
        when(claims.get("userId", String.class)).thenReturn("not-a-valid-uuid");
        when(claims.get("name", String.class)).thenReturn("John Doe");

        // Act
        jwtFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(response).sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication failed");
        verify(filterChain, never()).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void doFilterInternal_AdminRole_Success() throws ServletException, IOException {
        // Arrange
        when(request.getHeader("Authorization")).thenReturn("Bearer " + testToken);
        when(jwtUtil.verifyAndGetClaims(testToken)).thenReturn(claims);

        when(claims.get("role", String.class)).thenReturn("ADMIN");
        when(claims.get("isVerified", Boolean.class)).thenReturn(true);
        when(claims.get("userId", String.class)).thenReturn(testUserId.toString());
        when(claims.get("name", String.class)).thenReturn("Admin User");

        // Act
        jwtFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(filterChain).doFilter(request, response);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        assertEquals("ADMIN", userDetails.getRole());
        assertTrue(authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }
}