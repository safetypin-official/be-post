package com.safetypin.post.security;


import com.safetypin.post.exception.InvalidCredentialsException;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class JWTUtil {
    public Claims verifyAndGetClaims(String token) throws InvalidCredentialsException {return null;}
}
