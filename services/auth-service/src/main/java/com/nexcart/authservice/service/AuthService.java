package com.nexcart.authservice.service;

import com.nexcart.authservice.dto.LoginRequest;
import com.nexcart.authservice.dto.LoginResponse;
import com.nexcart.authservice.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

/**
 * Authentication Service
 * In production: integrate with user-service for real user validation
 * For now: demo users for testing
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    public LoginResponse authenticate(LoginRequest request) {
        // Note: Production version should integrate with user-service for credential validation
        // Current implementation uses demo users for testing purposes
        if (authenticateUser(request.getUsername(), request.getPassword())) {
            List<String> roles = getUserRoles(request.getUsername());
            String token = jwtTokenProvider.generateToken(request.getUsername(), roles);
            return new LoginResponse(token, request.getUsername(), roles);
        }
        throw new RuntimeException("Invalid credentials");
    }

    private boolean authenticateUser(String username, String password) {
        // Demo: admin/admin, user/user
        if ("admin".equals(username) && "admin".equals(password)) {
            return true;
        }
        if ("user".equals(username) && "user".equals(password)) {
            return true;
        }
        return false;
    }

    private List<String> getUserRoles(String username) {
        if ("admin".equals(username)) {
            return Arrays.asList("ROLE_ADMIN", "ROLE_USER");
        }
        return List.of("ROLE_USER");
    }

    public boolean validateToken(String token) {
        try {
            jwtTokenProvider.validateAndGetClaims(token);
            return !jwtTokenProvider.isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }
}
