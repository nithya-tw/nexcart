package com.nexcart.authservice.service;

import com.nexcart.authservice.dto.LoginRequest;
import com.nexcart.authservice.dto.LoginResponse;
import com.nexcart.authservice.security.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive Unit Tests for AuthService
 * 
 * This test class demonstrates JWT authentication patterns and best practices:
 * 1. JWT Tokens are stateless - no session storage needed
 * 2. Tokens contain claims (username, roles) signed with a secret key
 * 3. Token validation requires checking signature and expiration
 * 4. Role-based access control (RBAC) is embedded in the token claims
 * 
 * Mocking Strategy:
 * - JwtTokenProvider is mocked because it's an external dependency
 * - PasswordEncoder is mocked to avoid actual password hashing in tests
 * - This allows us to test AuthService business logic in isolation
 * 
 * Test Structure:
 * - Setup: Create test data and configure mocks
 * - Execute: Call the method under test
 * - Verify: Assert behavior and mock interactions
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Unit Tests")
class AuthServiceTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    // Test constants
    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PASSWORD = "admin";
    private static final String USER_USERNAME = "user";
    private static final String USER_PASSWORD = "user";
    private static final String INVALID_USERNAME = "invalid";
    private static final String INVALID_PASSWORD = "wrong";
    private static final String JWT_SECRET = "my-secret-key-that-is-long-enough-for-hs256-algorithm";
    private static final String VALID_JWT_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJhZG1pbiIsImlhdCI6MTUxNjIzOTAyMn0.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
    private static final String INVALID_JWT_TOKEN = "invalid.token.format";
    private static final String EXPIRED_JWT_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJleHAiOjE1MTYyMzQwMjJ9.expired";

    // Mock token for testing
    private String mockToken;
    private List<String> adminRoles;
    private List<String> userRoles;

    @BeforeEach
    void setUp() {
        /**
         * Setup method initializes test data before each test method
         * Why separate setup? 
         * - Each test should start with a clean state
         * - Prevents test pollution (tests affecting each other)
         * - Makes each test more readable and isolated
         */
        adminRoles = Arrays.asList("ROLE_ADMIN", "ROLE_USER");
        userRoles = Arrays.asList("ROLE_USER");
        mockToken = "mock-jwt-token-xyz";
    }

    // ============================================================================
    // AUTHENTICATE METHOD TESTS
    // ============================================================================

    /**
     * Test Case 1: Authenticate Admin User with Valid Credentials
     * 
     * JWT Pattern: Token contains admin roles
     * - Admins get both ROLE_ADMIN and ROLE_USER
     * - This demonstrates role hierarchy
     * 
     * What we're testing:
     * 1. Login with admin credentials succeeds
     * 2. Token is generated with correct username
     * 3. Admin roles are properly assigned
     */
    @Test
    @DisplayName("Should authenticate admin user with valid credentials and assign both ROLE_ADMIN and ROLE_USER")
    void testAuthenticateAdminUserSuccess() {
        // Arrange: Setup test data
        LoginRequest request = new LoginRequest();
        request.setUsername(ADMIN_USERNAME);
        request.setPassword(ADMIN_PASSWORD);

        // Mock: Tell JwtTokenProvider to return a token when asked
        when(jwtTokenProvider.generateToken(ADMIN_USERNAME, adminRoles))
                .thenReturn(mockToken);

        // Act: Call the authenticate method
        LoginResponse response = authService.authenticate(request);

        // Assert: Verify the response contains expected values
        assertThat(response)
                .as("Login response should not be null")
                .isNotNull();

        assertThat(response.getToken())
                .as("Token should be the mock token returned by JwtTokenProvider")
                .isEqualTo(mockToken);

        assertThat(response.getUsername())
                .as("Username should match request")
                .isEqualTo(ADMIN_USERNAME);

        assertThat(response.getRoles())
                .as("Admin should have both ROLE_ADMIN and ROLE_USER")
                .containsExactly("ROLE_ADMIN", "ROLE_USER")
                .hasSize(2);

        // Verify: Check that JwtTokenProvider was called correctly
        verify(jwtTokenProvider, times(1))
                .generateToken(ADMIN_USERNAME, adminRoles);
    }

    /**
     * Test Case 2: Authenticate Regular User with Valid Credentials
     * 
     * JWT Pattern: Token contains user roles only
     * - Regular users get only ROLE_USER
     * - This demonstrates role-based access control (RBAC)
     * 
     * What we're testing:
     * 1. Login with user credentials succeeds
     * 2. Token is generated with correct username
     * 3. User roles are properly assigned (not admin)
     */
    @Test
    @DisplayName("Should authenticate regular user with valid credentials and assign only ROLE_USER")
    void testAuthenticateRegularUserSuccess() {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setUsername(USER_USERNAME);
        request.setPassword(USER_PASSWORD);

        when(jwtTokenProvider.generateToken(USER_USERNAME, userRoles))
                .thenReturn(mockToken);

        // Act
        LoginResponse response = authService.authenticate(request);

        // Assert
        assertThat(response)
                .isNotNull();

        assertThat(response.getToken())
                .isEqualTo(mockToken);

        assertThat(response.getUsername())
                .isEqualTo(USER_USERNAME);

        assertThat(response.getRoles())
                .as("Regular user should have only ROLE_USER")
                .containsExactly("ROLE_USER")
                .hasSize(1)
                .doesNotContain("ROLE_ADMIN");

        verify(jwtTokenProvider, times(1))
                .generateToken(USER_USERNAME, userRoles);
    }

    /**
     * Test Case 3: Authenticate with Invalid Credentials
     * 
     * Error Handling Pattern: What happens when authentication fails?
     * - Invalid credentials should throw an exception
     * - This prevents unauthorized access
     * - JwtTokenProvider should NOT be called
     * 
     * What we're testing:
     * 1. Invalid username/password combination is rejected
     * 2. No token is generated for invalid credentials
     * 3. Exception is thrown to client
     */
    @Test
    @DisplayName("Should throw exception when authenticating with invalid credentials")
    void testAuthenticateWithInvalidCredentials() {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setUsername(INVALID_USERNAME);
        request.setPassword(INVALID_PASSWORD);

        // Act & Assert: Expect RuntimeException to be thrown
        assertThatThrownBy(() -> authService.authenticate(request))
                .as("Invalid credentials should throw RuntimeException")
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Invalid credentials")
                .hasNoCause();

        // Verify: JwtTokenProvider should NOT be called for invalid credentials
        verify(jwtTokenProvider, never())
                .generateToken(anyString(), anyList());
    }

    /**
     * Test Case 4: Authenticate with Wrong Password
     * 
     * Security Pattern: Wrong password is treated same as invalid user
     * - Both result in authentication failure
     * - No token is generated
     * 
     * What we're testing:
     * 1. Correct username but wrong password fails
     * 2. Exception is thrown
     * 3. Token generation is not called
     */
    @Test
    @DisplayName("Should throw exception when authenticating with correct username but wrong password")
    void testAuthenticateWithWrongPassword() {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setUsername(ADMIN_USERNAME);
        request.setPassword(INVALID_PASSWORD); // Wrong password

        // Act & Assert
        assertThatThrownBy(() -> authService.authenticate(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Invalid credentials");

        // Verify
        verify(jwtTokenProvider, never())
                .generateToken(anyString(), anyList());
    }

    /**
     * Test Case 5: Authenticate with Wrong Username
     * 
     * Security Pattern: Non-existent user is rejected
     * - Prevents user enumeration attacks (to some extent)
     * - Generic error message doesn't reveal if user exists
     * 
     * What we're testing:
     * 1. Non-existent username is rejected
     * 2. Exception is thrown
     * 3. Token generation is not called
     */
    @Test
    @DisplayName("Should throw exception when authenticating with non-existent username")
    void testAuthenticateWithNonExistentUsername() {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setUsername("non-existent-user");
        request.setPassword(ADMIN_PASSWORD);

        // Act & Assert
        assertThatThrownBy(() -> authService.authenticate(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Invalid credentials");

        // Verify
        verify(jwtTokenProvider, never())
                .generateToken(anyString(), anyList());
    }

    // ============================================================================
    // TOKEN VALIDATION TESTS
    // ============================================================================

    /**
     * Test Case 6: Validate Valid JWT Token
     * 
     * JWT Validation Pattern: How to verify a token is legitimate?
     * 1. Check signature (proves it wasn't tampered with)
     * 2. Check expiration (proves it's still valid)
     * 
     * Why both checks?
     * - Signature verification ensures token integrity
     * - Expiration check prevents old tokens from being reused
     * 
     * What we're testing:
     * 1. Valid token passes validation
     * 2. Returns true when token is valid
     * 3. Both signature and expiration checks pass
     */
    @Test
    @DisplayName("Should validate a valid JWT token and return true")
    void testValidateValidToken() {
        // Arrange
        // Mock Claims object (JWT payload)
        Claims mockClaims = mock(Claims.class);
        
        // Configure mocks: valid token with future expiration
        when(jwtTokenProvider.validateAndGetClaims(mockToken))
                .thenReturn(mockClaims); // No exception means signature is valid
        when(jwtTokenProvider.isTokenExpired(mockToken))
                .thenReturn(false); // Token is not expired

        // Act: Validate the token
        boolean isValid = authService.validateToken(mockToken);

        // Assert
        assertThat(isValid)
                .as("Valid token should return true")
                .isTrue();

        // Verify: Both validation methods were called
        verify(jwtTokenProvider, times(1))
                .validateAndGetClaims(mockToken);
        verify(jwtTokenProvider, times(1))
                .isTokenExpired(mockToken);
    }

    /**
     * Test Case 7: Validate Invalid JWT Token (Tampered)
     * 
     * JWT Security Pattern: What if token signature is invalid?
     * - Invalid signature means someone tampered with the token
     * - Could also be caused by wrong secret key
     * - Token should be rejected immediately
     * 
     * What we're testing:
     * 1. Invalid/tampered token is rejected
     * 2. Returns false when token is invalid
     * 3. Exception from JwtTokenProvider is caught and handled
     */
    @Test
    @DisplayName("Should return false when validating an invalid JWT token")
    void testValidateInvalidToken() {
        // Arrange
        // Mock JwtTokenProvider to throw exception (invalid signature)
        when(jwtTokenProvider.validateAndGetClaims(INVALID_JWT_TOKEN))
                .thenThrow(new RuntimeException("Invalid token signature"));

        // Act: Try to validate the invalid token
        boolean isValid = authService.validateToken(INVALID_JWT_TOKEN);

        // Assert
        assertThat(isValid)
                .as("Invalid token should return false")
                .isFalse();

        // Verify: validateAndGetClaims was called (it threw exception)
        verify(jwtTokenProvider, times(1))
                .validateAndGetClaims(INVALID_JWT_TOKEN);
        // isTokenExpired should NOT be called because exception was thrown first
        verify(jwtTokenProvider, never())
                .isTokenExpired(INVALID_JWT_TOKEN);
    }

    /**
     * Test Case 8: Validate Expired JWT Token
     * 
     * JWT Expiration Pattern: Tokens have a lifetime
     * - After expiration, token is no longer valid
     * - Even if signature is valid, expired tokens are rejected
     * - This forces users to re-authenticate periodically
     * 
     * Why expiration?
     * - Limits window of exposure if token is compromised
     * - Prevents unlimited token reuse
     * - Common expiration times: 15 minutes to 1 hour
     * 
     * What we're testing:
     * 1. Expired token is rejected
     * 2. Returns false even if signature is valid
     * 3. Expiration check is properly performed
     */
    @Test
    @DisplayName("Should return false when validating an expired JWT token")
    void testValidateExpiredToken() {
        // Arrange
        Claims mockClaims = mock(Claims.class);
        
        // Configure mocks: signature is valid but token is expired
        when(jwtTokenProvider.validateAndGetClaims(EXPIRED_JWT_TOKEN))
                .thenReturn(mockClaims); // Signature validation passes
        when(jwtTokenProvider.isTokenExpired(EXPIRED_JWT_TOKEN))
                .thenReturn(true); // But token is expired

        // Act: Try to validate the expired token
        boolean isValid = authService.validateToken(EXPIRED_JWT_TOKEN);

        // Assert
        assertThat(isValid)
                .as("Expired token should return false")
                .isFalse();

        // Verify: Both validation steps were performed
        verify(jwtTokenProvider, times(1))
                .validateAndGetClaims(EXPIRED_JWT_TOKEN);
        verify(jwtTokenProvider, times(1))
                .isTokenExpired(EXPIRED_JWT_TOKEN);
    }

    // ============================================================================
    // ROLE ASSIGNMENT TESTS
    // ============================================================================

    /**
     * Test Case 9: Verify Admin Gets Both ROLE_ADMIN and ROLE_USER
     * 
     * Authorization Pattern: Role Hierarchy
     * - Admins are also users (have both roles)
     * - This prevents code duplication
     * - Allows admins to perform user operations
     * 
     * Real-world example:
     * - Admin can view their own orders (user permission)
     * - Admin can also view all orders (admin permission)
     * 
     * What we're testing:
     * 1. Admin authentication assigns correct roles
     * 2. Admin has both ROLE_ADMIN and ROLE_USER
     * 3. Roles are in correct order
     */
    @Test
    @DisplayName("Should assign both ROLE_ADMIN and ROLE_USER to admin users")
    void testAdminRoleAssignmentIncludesUserRole() {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setUsername(ADMIN_USERNAME);
        request.setPassword(ADMIN_PASSWORD);

        when(jwtTokenProvider.generateToken(ADMIN_USERNAME, adminRoles))
                .thenReturn(mockToken);

        // Act
        LoginResponse response = authService.authenticate(request);

        // Assert
        assertThat(response.getRoles())
                .as("Admin roles should include both ROLE_ADMIN and ROLE_USER")
                .hasSize(2)
                .containsExactly("ROLE_ADMIN", "ROLE_USER")
                .contains("ROLE_ADMIN")
                .contains("ROLE_USER");

        // Verify: Token generation includes both roles
        verify(jwtTokenProvider, times(1))
                .generateToken(eq(ADMIN_USERNAME), eq(adminRoles));
    }

    /**
     * Test Case 10: Verify Regular User Gets Only ROLE_USER
     * 
     * Authorization Pattern: Principle of Least Privilege
     * - Users only get permissions they need
     * - Regular users don't get admin role
     * - This prevents privilege escalation
     * 
     * Security benefit:
     * - Even if a user account is compromised, damage is limited
     * - User can't accidentally access admin features
     * 
     * What we're testing:
     * 1. Regular user authentication assigns only ROLE_USER
     * 2. User does NOT get ROLE_ADMIN
     * 3. Role list has exactly one element
     */
    @Test
    @DisplayName("Should assign only ROLE_USER to regular users without ROLE_ADMIN")
    void testRegularUserRoleAssignmentDoesNotIncludeAdminRole() {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setUsername(USER_USERNAME);
        request.setPassword(USER_PASSWORD);

        when(jwtTokenProvider.generateToken(USER_USERNAME, userRoles))
                .thenReturn(mockToken);

        // Act
        LoginResponse response = authService.authenticate(request);

        // Assert
        assertThat(response.getRoles())
                .as("Regular user should have only ROLE_USER, not ROLE_ADMIN")
                .hasSize(1)
                .containsExactly("ROLE_USER")
                .doesNotContain("ROLE_ADMIN");

        // Verify: Token generation uses user roles (not admin roles)
        verify(jwtTokenProvider, times(1))
                .generateToken(eq(USER_USERNAME), eq(userRoles));
    }

    // ============================================================================
    // EDGE CASES AND ADDITIONAL TESTS
    // ============================================================================

    /**
     * Test Case 11: Validate Token With Null Claims Exception
     * 
     * Error Handling Pattern: Handle unexpected exceptions gracefully
     * - JwtTokenProvider might throw different exceptions
     * - AuthService should catch them all and return false
     * - Prevents cascading failures
     * 
     * What we're testing:
     * 1. Unexpected exceptions are caught
     * 2. Method returns false instead of throwing
     * 3. Exception message doesn't leak security info
     */
    @Test
    @DisplayName("Should handle unexpected exceptions during token validation gracefully")
    void testValidateTokenHandlesUnexpectedException() {
        // Arrange
        String problematicToken = "some-problematic-token";
        
        // Mock: Throw unexpected exception
        when(jwtTokenProvider.validateAndGetClaims(problematicToken))
                .thenThrow(new IllegalArgumentException("Unexpected error"));

        // Act: Validate token with exception
        boolean isValid = authService.validateToken(problematicToken);

        // Assert: Should return false, not throw exception
        assertThat(isValid)
                .as("Should handle exceptions gracefully and return false")
                .isFalse();

        // Verify: Method attempted validation
        verify(jwtTokenProvider, times(1))
                .validateAndGetClaims(problematicToken);
    }

    /**
     * Test Case 12: Authenticate Returns LoginResponse With All Fields Set
     * 
     * Data Integrity Pattern: Response contains all required information
     * - Client needs token to make authenticated requests
     * - Client needs username for UI display
     * - Client needs roles for authorization decisions
     * 
     * What we're testing:
     * 1. All response fields are populated
     * 2. No null values in response
     * 3. Response object is properly constructed
     */
    @Test
    @DisplayName("Should return LoginResponse with all required fields populated for admin")
    void testAuthenticateResponseHasAllRequiredFields() {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setUsername(ADMIN_USERNAME);
        request.setPassword(ADMIN_PASSWORD);

        when(jwtTokenProvider.generateToken(ADMIN_USERNAME, adminRoles))
                .thenReturn(mockToken);

        // Act
        LoginResponse response = authService.authenticate(request);

        // Assert: All fields are present and non-null
        assertThat(response)
                .isNotNull()
                .hasFieldOrPropertyWithValue("token", mockToken)
                .hasFieldOrPropertyWithValue("username", ADMIN_USERNAME);

        assertThat(response.getRoles())
                .isNotNull()
                .isNotEmpty();

        // Additional assertions for field values
        assertThat(response.getToken())
                .isNotBlank()
                .isEqualTo(mockToken);

        assertThat(response.getUsername())
                .isNotBlank()
                .isEqualTo(ADMIN_USERNAME);

        assertThat(response.getRoles())
                .containsExactly("ROLE_ADMIN", "ROLE_USER");
    }
}
