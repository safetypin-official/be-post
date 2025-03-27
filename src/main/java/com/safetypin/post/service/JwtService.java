package com.safetypin.post.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.apache.hc.client5.http.auth.InvalidCredentialsException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.UUID;

@Service
@Configuration
public class JwtService {

    private final Key key;

    public JwtService(@Value("${jwt.secret:120c727764e959b6f15dfd537c43bde23a70c18f82318b4633009afbbdd8e250}") String secretKey) {
        this.key = Keys.hmacShaKeyFor(secretKey.getBytes());
        System.out.println("SecretKey: " + secretKey);
    }

    public Claims parseToken(String token) throws InvalidCredentialsException {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException ex) {
            throw new InvalidCredentialsException("Token expired", ex);
        } catch (Exception e) {
            throw new InvalidCredentialsException("JWT Access Token parsing failed");
        }

    }

    public UUID getUserIdFromJwtToken(String token) throws InvalidCredentialsException {
        Claims claims = parseToken(token);

        // handle no expired date
        if (claims.getExpiration() == null) {
            throw new InvalidCredentialsException("Token has no expired date");
        }

        // handle valid uuid
        UUID userId;
        try {
            userId = UUID.fromString(claims.getSubject());
        } catch (IllegalArgumentException e) {
            throw new InvalidCredentialsException("Invalid subject");
        }

        return userId;
    }

    // this is the method that's being called by outside
    public UUID getUserIdFromAuthorizationHeader(String authorizationHeader) throws InvalidCredentialsException {
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            String token = authorizationHeader.substring(7);
            return getUserIdFromJwtToken(token);
        }
        throw new InvalidCredentialsException("Authorization header is invalid");
    }
}
