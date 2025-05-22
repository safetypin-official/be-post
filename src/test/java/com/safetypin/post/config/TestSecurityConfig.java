package com.safetypin.post.config;

import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

import com.safetypin.post.security.JWTFilter;
import com.safetypin.post.security.JWTUtil;
import com.safetypin.post.service.AdminService;

@TestConfiguration
public class TestSecurityConfig {

    @Bean
    @Primary
    public AdminService adminService() {
        return Mockito.mock(AdminService.class);
    }

    @Bean
    @Primary
    public JWTUtil jwtUtil() {
        return Mockito.mock(JWTUtil.class);
    }

    @Bean
    @Primary
    public JWTFilter jwtFilter() {
        return Mockito.mock(JWTFilter.class);
    }

    @Bean
    @Primary
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll());
        return http.build();
    }
}
