package com.nexcart.apigateway.filter;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@DisplayName("JwtAuthenticationFilter Tests")
class JwtAuthenticationFilterTest {

    private static final String JWT_SECRET = "mySecretKeyForJWTTokenGenerationThatIsAtLeast256BitsLong";
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    private GatewayFilterChain filterChain;

    @BeforeEach
    void setUp() {
        jwtAuthenticationFilter = new JwtAuthenticationFilter();
        ReflectionTestUtils.setField(jwtAuthenticationFilter, "jwtSecret", JWT_SECRET);
        filterChain = mock(GatewayFilterChain.class);
        when(filterChain.filter(any())).thenReturn(Mono.empty());
    }

    @Test
    @DisplayName("Should reject request without Authorization header")
    void shouldRejectRequestWithoutAuthHeader() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/test").build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        GatewayFilter filter = jwtAuthenticationFilter.apply(new JwtAuthenticationFilter.Config());
        Mono<Void> result = filter.filter(exchange, filterChain);

        result.block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(filterChain, never()).filter(any());
    }

    @Test
    @DisplayName("Should reject request with malformed Authorization header")
    void shouldRejectRequestWithMalformedAuthHeader() {
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/test")
                .header("Authorization", "InvalidToken")
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        GatewayFilter filter = jwtAuthenticationFilter.apply(new JwtAuthenticationFilter.Config());
        Mono<Void> result = filter.filter(exchange, filterChain);

        result.block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(filterChain, never()).filter(any());
    }

    @Test
    @DisplayName("Should reject request with invalid JWT token")
    void shouldRejectRequestWithInvalidToken() {
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/test")
                .header("Authorization", "Bearer invalid.jwt.token")
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        GatewayFilter filter = jwtAuthenticationFilter.apply(new JwtAuthenticationFilter.Config());
        Mono<Void> result = filter.filter(exchange, filterChain);

        result.block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(filterChain, never()).filter(any());
    }

    @Test
    @DisplayName("Should allow request with valid JWT token")
    void shouldAllowRequestWithValidToken() {
        String token = generateValidToken("testuser");
        
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/test")
                .header("Authorization", "Bearer " + token)
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        GatewayFilter filter = jwtAuthenticationFilter.apply(new JwtAuthenticationFilter.Config());
        Mono<Void> result = filter.filter(exchange, filterChain);

        result.block();

        verify(filterChain).filter(exchange);
    }

    private String generateValidToken(String username) {
        SecretKey key = Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject(username)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 86400000))
                .signWith(key)
                .compact();
    }
}
