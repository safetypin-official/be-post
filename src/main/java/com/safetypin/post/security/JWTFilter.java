package com.safetypin.post.security;

import com.safetypin.post.dto.UserDetails;
import com.safetypin.post.exception.InvalidCredentialsException;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Slf4j
@Component
public class JWTFilter extends OncePerRequestFilter {
    private final JWTUtil jwtUtil;

    public JWTFilter(JWTUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        // If token doesn't exist, continue as normal (without authentication)
        if (authHeader == null || authHeader.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        // Check token is in the correct format
        if (!authHeader.startsWith("Bearer ")) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication token must start with 'Bearer '");
            return;
        }
        String jwtToken = authHeader.substring(7);

        // Verify the JWT token
        try {
            // Verify and get claims from token
            Claims claims = jwtUtil.verifyAndGetClaims(jwtToken);

            // Convert claims to UserDetails
            UserDetails userDetails = UserDetails.fromClaims(claims);

            // Create pre-authenticated token with authorities based on user role
            PreAuthenticatedAuthenticationToken authentication = new PreAuthenticatedAuthenticationToken(
                    userDetails,
                    jwtToken, // credential (the token itself)
                    Collections.singletonList(new SimpleGrantedAuthority(userDetails.getRole().toString()))
            );

            // Set authentication in context
            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (InvalidCredentialsException e) {
            // Send and log error if token is invalid
            log.info("Invalid JWT token due to: {}", e.getMessage());
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid JWT token");
            return;
        } catch (Exception e) {
            // Other exceptions relating to setting the authentication in the security context
            log.error("Could not set user authentication in security context", e);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication failed");
            return;
        }

        // Log the successful authentication
        log.info("JWT token is valid for user: {}", SecurityContextHolder.getContext().getAuthentication().getName());

        // Continue the filter chain if token is valid
        filterChain.doFilter(request, response);
    }
}
