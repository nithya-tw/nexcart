package com.nexcart.authservice.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 📚 CONCEPT: JWT Token Provider Tests
 * 🎯 ANALOGY: Like testing a ticket booth - does it create valid tickets and reject fakes?
 * 
 * Why test JWT? Security is critical - one bug = potential data breach
 * Real-world: Auth0 has extensive JWT testing - tokens must be cryptographically secure
 */
@DisplayName("JwtTokenProvider Tests")
class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    
    // Must be at least 256 bits (32 bytes) for HS256
    private static final String TEST_SECRET = "testsecretkeythatisatleast256bits1234567890abcdefghijklmnop";
    private static final long TEST_EXPIRATION = 60000; // 1 minute

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtSecret", TEST_SECRET);
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtExpiration", TEST_EXPIRATION);
    }

    @Test
    @DisplayName("Should generate valid JWT token with username and roles")
    void shouldGenerateValidJwtTokenWithUsernameAndRoles() {
        // ARRANGE
        String username = "john.doe@example.com";
        List<String> roles = List.of("ROLE_USER", "ROLE_ADMIN");
        
        // ACT
        String token = jwtTokenProvider.generateToken(username, roles);
        
        // ASSERT
        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        assertThat(token.split("\\.")).hasSize(3); // JWT has 3 parts: header.payload.signature
    }

    @Test
    @DisplayName("Should extract username from valid token")
    void shouldExtractUsernameFromValidToken() {
        // ARRANGE
        String username = "john.doe@example.com";
        List<String> roles = List.of("ROLE_USER");
        String token = jwtTokenProvider.generateToken(username, roles);
        
        // ACT
        String extractedUsername = jwtTokenProvider.getUsernameFromToken(token);
        
        // ASSERT
        assertThat(extractedUsername).isEqualTo(username);
    }

    @Test
    @DisplayName("Should extract roles from token claims")
    void shouldExtractRolesFromTokenClaims() {
        // ARRANGE
        String username = "john.doe@example.com";
        List<String> roles = List.of("ROLE_USER", "ROLE_ADMIN");
        String token = jwtTokenProvider.generateToken(username, roles);
        
        // ACT
        Claims claims = jwtTokenProvider.validateAndGetClaims(token);
        
        // ASSERT
        assertThat(claims.getSubject()).isEqualTo(username);
        assertThat(claims.get("roles")).isInstanceOf(List.class);
        @SuppressWarnings("unchecked")
        List<String> extractedRoles = (List<String>) claims.get("roles");
        assertThat(extractedRoles).containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
    }

    @Test
    @DisplayName("Should return false for non-expired token")
    void shouldReturnFalseForNonExpiredToken() {
        // ARRANGE
        String username = "john.doe@example.com";
        List<String> roles = List.of("ROLE_USER");
        String token = jwtTokenProvider.generateToken(username, roles);
        
        // ACT
        boolean expired = jwtTokenProvider.isTokenExpired(token);
        
        // ASSERT
        assertThat(expired).isFalse();
    }

    @Test
    @DisplayName("Should return true for expired token")
    void shouldReturnTrueForExpiredToken() throws InterruptedException {
        // ARRANGE
        jwtTokenProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtSecret", TEST_SECRET);
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtExpiration", 100L); // 100ms
        
        String username = "john.doe@example.com";
        List<String> roles = List.of("ROLE_USER");
        String token = jwtTokenProvider.generateToken(username, roles);
        
        // ACT - Wait for token to expire
        Thread.sleep(200); // Wait 200ms
        
        // ASSERT
        assertThatThrownBy(() -> jwtTokenProvider.isTokenExpired(token))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    @DisplayName("Should generate different tokens for different users")
    void shouldGenerateDifferentTokensForDifferentUsers() {
        // ARRANGE
        String user1 = "user1@example.com";
        String user2 = "user2@example.com";
        List<String> roles = List.of("ROLE_USER");
        
        // ACT
        String token1 = jwtTokenProvider.generateToken(user1, roles);
        String token2 = jwtTokenProvider.generateToken(user2, roles);
        
        // ASSERT
        assertThat(token1).isNotEqualTo(token2);
    }

    @Test
    @DisplayName("Should generate different tokens for same user at different times")
    void shouldGenerateDifferentTokensForSameUserAtDifferentTimes() throws InterruptedException {
        // ARRANGE
        String username = "john.doe@example.com";
        List<String> roles = List.of("ROLE_USER");
        
        // ACT
        String token1 = jwtTokenProvider.generateToken(username, roles);
        Thread.sleep(1100); // Wait > 1 second to ensure different issuedAt time
        String token2 = jwtTokenProvider.generateToken(username, roles);
        
        // ASSERT
        assertThat(token1).isNotEqualTo(token2);
    }

    @Test
    @DisplayName("Should include issued at and expiration dates in claims")
    void shouldIncludeIssuedAtAndExpirationDatesInClaims() {
        // ARRANGE
        String username = "john.doe@example.com";
        List<String> roles = List.of("ROLE_USER");
        String token = jwtTokenProvider.generateToken(username, roles);
        
        // ACT
        Claims claims = jwtTokenProvider.validateAndGetClaims(token);
        
        // ASSERT
        assertThat(claims.getIssuedAt()).isNotNull();
        assertThat(claims.getExpiration()).isNotNull();
        assertThat(claims.getExpiration()).isAfter(claims.getIssuedAt());
    }

    @Test
    @DisplayName("Should throw exception for invalid token")
    void shouldThrowExceptionForInvalidToken() {
        // ARRANGE
        String invalidToken = "invalid.token.here";
        
        // ACT & ASSERT
        assertThatThrownBy(() -> jwtTokenProvider.validateAndGetClaims(invalidToken))
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("Should throw exception for tampered token")
    void shouldThrowExceptionForTamperedToken() {
        // ARRANGE
        String username = "john.doe@example.com";
        List<String> roles = List.of("ROLE_USER");
        String validToken = jwtTokenProvider.generateToken(username, roles);
        
        // Tamper with the token by changing a character
        String tamperedToken = validToken.substring(0, validToken.length() - 5) + "XXXXX";
        
        // ACT & ASSERT
        assertThatThrownBy(() -> jwtTokenProvider.validateAndGetClaims(tamperedToken))
                .isInstanceOf(Exception.class);
    }
}
