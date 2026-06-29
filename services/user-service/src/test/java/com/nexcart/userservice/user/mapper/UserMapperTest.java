package com.nexcart.userservice.user.mapper;

import com.nexcart.userservice.user.dto.request.CreateUserRequest;
import com.nexcart.userservice.user.dto.request.UpdateUserRequest;
import com.nexcart.userservice.user.dto.response.UserResponse;
import com.nexcart.userservice.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for UserMapper class.
 *
 * ===== EDUCATIONAL CONTEXT =====
 * DTO (Data Transfer Object) Pattern:
 * - DTOs are used to transfer data between application layers (controller, service, database)
 * - They decouple external representations from internal domain models
 * - This allows layers to evolve independently without breaking contracts
 *
 * Why Mappers Isolate Layers:
 * - Mappers act as explicit conversion logic between entities and DTOs
 * - They encapsulate transformation rules in one place
 * - This prevents business logic from spreading across layers
 * - Changes to entity structure only require mapper updates, not widespread refactoring
 * - Improves testability: mappers can be tested independently without database or framework
 *
 * This test suite focuses on data integrity during mapping, ensuring:
 * - All fields are correctly transferred from source to destination
 * - No field is lost, corrupted, or mismapped
 * - Null values are handled appropriately (where applicable)
 */
@DisplayName("UserMapper Tests - Verify DTO/Entity Conversions")
class UserMapperTest {

    private UserMapper userMapper;

    @BeforeEach
    void setUp() {
        // Instantiate UserMapper without Spring context - testing pure mapping logic
        userMapper = new UserMapper();
    }

    /**
     * Test suite for toResponse(User) method.
     * This method converts domain User entities to UserResponse DTOs for API responses.
     * Purpose: Verify all entity fields map correctly to response fields and IDs are preserved.
     */
    @Nested
    @DisplayName("toResponse(User) - Entity to Response DTO Conversion")
    class ToResponseTests {

        @Test
        @DisplayName("should map all non-null user fields to response correctly")
        void testToResponse_MapsAllFieldsCorrectly() {
            // Arrange: Create a complete User entity with all fields populated
            User user = User.builder()
                    .id(1L)
                    .firstName("John")
                    .lastName("Doe")
                    .email("john.doe@example.com")
                    .phoneNumber("9876543210")
                    .build();

            // Act: Convert entity to response DTO
            UserResponse response = userMapper.toResponse(user);

            // Assert: Verify all fields are correctly mapped
            // Each assertion documents the expected field mapping behavior
            assertThat(response)
                    .isNotNull()
                    .extracting(
                            UserResponse::getId,
                            UserResponse::getFirstName,
                            UserResponse::getLastName,
                            UserResponse::getEmail,
                            UserResponse::getPhoneNumber
                    )
                    .containsExactly(
                            1L,
                            "John",
                            "Doe",
                            "john.doe@example.com",
                            "9876543210"
                    );
        }

        @Test
        @DisplayName("should preserve user ID during conversion")
        void testToResponse_PreservesUserId() {
            // Arrange: Create user with specific ID
            User user = User.builder()
                    .id(42L)
                    .firstName("Jane")
                    .lastName("Smith")
                    .email("jane.smith@example.com")
                    .phoneNumber("9123456789")
                    .build();

            // Act
            UserResponse response = userMapper.toResponse(user);

            // Assert: ID must be exactly preserved (no mutations, calculations, or defaults)
            assertThat(response.getId()).isEqualTo(42L);
        }

        @Test
        @DisplayName("should map user with special characters in names and email")
        void testToResponse_HandlesSpecialCharacters() {
            // Arrange: Create user with special characters (apostrophes, hyphens, dots)
            // This tests robustness: mappers should handle any valid string content
            User user = User.builder()
                    .id(5L)
                    .firstName("Jean-Pierre")
                    .lastName("O'Connor")
                    .email("jean.pierre.oconnor+test@example.co.uk")
                    .phoneNumber("9876543210")
                    .build();

            // Act
            UserResponse response = userMapper.toResponse(user);

            // Assert: Special characters should pass through unchanged
            assertThat(response.getFirstName()).isEqualTo("Jean-Pierre");
            assertThat(response.getLastName()).isEqualTo("O'Connor");
            assertThat(response.getEmail()).isEqualTo("jean.pierre.oconnor+test@example.co.uk");
        }

        @Test
        @DisplayName("should handle edge case: numeric names")
        void testToResponse_NumericNames() {
            // Arrange: While unusual, some cultures have numeric components in names
            User user = User.builder()
                    .id(10L)
                    .firstName("3PO")
                    .lastName("C3PO")
                    .email("c3po@example.com")
                    .phoneNumber("1234567890")
                    .build();

            // Act
            UserResponse response = userMapper.toResponse(user);

            // Assert: Numeric strings should be preserved as-is
            assertThat(response.getFirstName()).isEqualTo("3PO");
            assertThat(response.getLastName()).isEqualTo("C3PO");
        }

        @Test
        @DisplayName("should map whitespace-padded values without trimming")
        void testToResponse_PreservesWhitespace() {
            // Arrange: Values with leading/trailing whitespace
            // Testing whether mapper respects data as-is (validation is controller's job)
            User user = User.builder()
                    .id(7L)
                    .firstName("  John  ")
                    .lastName(" Doe ")
                    .email("john@example.com")
                    .phoneNumber("9876543210")
                    .build();

            // Act
            UserResponse response = userMapper.toResponse(user);

            // Assert: Whitespace should be preserved (trimming is validation concern, not mapping)
            assertThat(response.getFirstName()).isEqualTo("  John  ");
            assertThat(response.getLastName()).isEqualTo(" Doe ");
        }
    }

    /**
     * Test suite for toEntity(CreateUserRequest) method.
     * Converts incoming API requests to domain entities for persistence.
     * Purpose: Verify request fields map to entity and null values are handled.
     */
    @Nested
    @DisplayName("toEntity(CreateUserRequest) - Request DTO to Entity Conversion")
    class ToEntityFromCreateRequestTests {

        @Test
        @DisplayName("should map all create request fields to entity correctly")
        void testToEntity_CreateRequest_MapsAllFieldsCorrectly() {
            // Arrange: Build a complete CreateUserRequest with all fields
            CreateUserRequest request = CreateUserRequest.builder()
                    .firstName("Alice")
                    .lastName("Johnson")
                    .email("alice.johnson@example.com")
                    .phoneNumber("9988776655")
                    .build();

            // Act: Convert request to entity
            User entity = userMapper.toEntity(request);

            // Assert: All request fields should map to corresponding entity fields
            assertThat(entity)
                    .isNotNull()
                    .satisfies(e -> {
                        assertThat(e.getFirstName()).isEqualTo("Alice");
                        assertThat(e.getLastName()).isEqualTo("Johnson");
                        assertThat(e.getEmail()).isEqualTo("alice.johnson@example.com");
                        assertThat(e.getPhoneNumber()).isEqualTo("9988776655");
                    });
        }

        @Test
        @DisplayName("should not set ID when converting from create request")
        void testToEntity_CreateRequest_NoIdSet() {
            // Arrange: Create request doesn't include ID (it's assigned by database)
            CreateUserRequest request = CreateUserRequest.builder()
                    .firstName("Bob")
                    .lastName("Williams")
                    .email("bob@example.com")
                    .phoneNumber("9876543210")
                    .build();

            // Act
            User entity = userMapper.toEntity(request);

            // Assert: ID should be null (database will assign it)
            // This is critical for new entity creation - mapper must not generate IDs
            assertThat(entity.getId()).isNull();
        }

        @Test
        @DisplayName("should handle null values appropriately in create request")
        void testToEntity_CreateRequest_NullValues() {
            // Arrange: Create request with null fields
            // Note: Validation typically prevents nulls, but mapper should handle gracefully
            CreateUserRequest request = CreateUserRequest.builder()
                    .firstName("Charlie")
                    .lastName(null)  // null last name
                    .email("charlie@example.com")
                    .phoneNumber(null)  // null phone
                    .build();

            // Act
            User entity = userMapper.toEntity(request);

            // Assert: Null values should map as null (validation is upstream responsibility)
            assertThat(entity.getFirstName()).isEqualTo("Charlie");
            assertThat(entity.getLastName()).isNull();
            assertThat(entity.getEmail()).isEqualTo("charlie@example.com");
            assertThat(entity.getPhoneNumber()).isNull();
        }

        @Test
        @DisplayName("should preserve empty strings in create request")
        void testToEntity_CreateRequest_EmptyStrings() {
            // Arrange: Create request with empty string values
            // Validation prevents this in practice, but mapper should preserve as-is
            CreateUserRequest request = CreateUserRequest.builder()
                    .firstName("")
                    .lastName("Test")
                    .email("")
                    .phoneNumber("")
                    .build();

            // Act
            User entity = userMapper.toEntity(request);

            // Assert: Empty strings should be preserved (not converted to null)
            assertThat(entity.getFirstName()).isEmpty();
            assertThat(entity.getLastName()).isEqualTo("Test");
            assertThat(entity.getEmail()).isEmpty();
            assertThat(entity.getPhoneNumber()).isEmpty();
        }

        @Test
        @DisplayName("should handle maximum length values in create request")
        void testToEntity_CreateRequest_MaxLengthValues() {
            // Arrange: Create request with values at their length limits
            // This tests that mapper handles boundary values correctly
            String maxName = "A".repeat(50);  // 50 chars is max for firstName/lastName
            String longEmail = "a".repeat(50) + "@example.com";  // email has practical limit
            String phone = "9876543210";  // exactly 10 digits

            CreateUserRequest request = CreateUserRequest.builder()
                    .firstName(maxName)
                    .lastName(maxName)
                    .email(longEmail)
                    .phoneNumber(phone)
                    .build();

            // Act
            User entity = userMapper.toEntity(request);

            // Assert: Maximum length values should map completely
            assertThat(entity.getFirstName()).isEqualTo(maxName).hasSize(50);
            assertThat(entity.getLastName()).isEqualTo(maxName).hasSize(50);
            assertThat(entity.getEmail()).isEqualTo(longEmail);
            assertThat(entity.getPhoneNumber()).isEqualTo(phone);
        }
    }

    /**
     * Test suite for toEntity(UpdateUserRequest) method.
     * Converts update requests to entity instances for persistence layer updates.
     * Purpose: Verify update request fields map correctly (similar to create, but semantically different).
     */
    @Nested
    @DisplayName("toEntity(UpdateUserRequest) - Update Request DTO to Entity Conversion")
    class ToEntityFromUpdateRequestTests {

        @Test
        @DisplayName("should map all update request fields to entity correctly")
        void testToEntity_UpdateRequest_MapsAllFieldsCorrectly() {
            // Arrange: Build UpdateUserRequest with modified values
            UpdateUserRequest request = UpdateUserRequest.builder()
                    .firstName("David")
                    .lastName("Brown")
                    .email("david.brown@example.com")
                    .phoneNumber("8765432109")
                    .build();

            // Act: Convert update request to entity
            User entity = userMapper.toEntity(request);

            // Assert: All update request fields should map to entity
            assertThat(entity)
                    .isNotNull()
                    .satisfies(e -> {
                        assertThat(e.getFirstName()).isEqualTo("David");
                        assertThat(e.getLastName()).isEqualTo("Brown");
                        assertThat(e.getEmail()).isEqualTo("david.brown@example.com");
                        assertThat(e.getPhoneNumber()).isEqualTo("8765432109");
                    });
        }

        @Test
        @DisplayName("should not set ID when converting from update request")
        void testToEntity_UpdateRequest_NoIdSet() {
            // Arrange: Update request doesn't include ID (obtained from URL path)
            UpdateUserRequest request = UpdateUserRequest.builder()
                    .firstName("Eve")
                    .lastName("Green")
                    .email("eve@example.com")
                    .phoneNumber("9876543210")
                    .build();

            // Act
            User entity = userMapper.toEntity(request);

            // Assert: ID should remain null
            // Service layer will set the ID from the URL parameter before saving
            assertThat(entity.getId()).isNull();
        }

        @Test
        @DisplayName("should handle null and empty values in update request")
        void testToEntity_UpdateRequest_NullAndEmptyValues() {
            // Arrange: Update request may have null fields for optional updates
            UpdateUserRequest request = UpdateUserRequest.builder()
                    .firstName("Frank")
                    .lastName(null)
                    .email("frank@example.com")
                    .phoneNumber("")
                    .build();

            // Act
            User entity = userMapper.toEntity(request);

            // Assert: Preserve exact values (null and empty strings distinguished)
            assertThat(entity.getFirstName()).isEqualTo("Frank");
            assertThat(entity.getLastName()).isNull();
            assertThat(entity.getEmail()).isEqualTo("frank@example.com");
            assertThat(entity.getPhoneNumber()).isEmpty();
        }

        @Test
        @DisplayName("should map unchanged values in update request")
        void testToEntity_UpdateRequest_UnchangedValues() {
            // Arrange: Simulate update where some values are unchanged
            UpdateUserRequest request = UpdateUserRequest.builder()
                    .firstName("Grace")  // unchanged
                    .lastName("Henry")  // updated
                    .email("grace.henry@example.com")  // unchanged
                    .phoneNumber("9123456789")  // updated
                    .build();

            // Act
            User entity = userMapper.toEntity(request);

            // Assert: All values should map whether changed or not
            assertThat(entity.getFirstName()).isEqualTo("Grace");
            assertThat(entity.getLastName()).isEqualTo("Henry");
            assertThat(entity.getEmail()).isEqualTo("grace.henry@example.com");
            assertThat(entity.getPhoneNumber()).isEqualTo("9123456789");
        }
    }

    /**
     * Cross-mapper integration tests.
     * Verify round-trip conversions and consistency between different mapping methods.
     */
    @Nested
    @DisplayName("Cross-Mapper Integration - Round-trip and Consistency Tests")
    class CrossMapperTests {

        @Test
        @DisplayName("should support round-trip: CreateRequest -> Entity -> Response")
        void testRoundTrip_CreateRequestToResponse() {
            // Arrange: Start with a create request
            CreateUserRequest createRequest = CreateUserRequest.builder()
                    .firstName("Ivan")
                    .lastName("Jackson")
                    .email("ivan@example.com")
                    .phoneNumber("9876543210")
                    .build();

            // Act: Map to entity, then simulate database assignment of ID, then to response
            User entity = userMapper.toEntity(createRequest);
            entity.setId(100L);  // Simulate database ID assignment
            UserResponse response = userMapper.toResponse(entity);

            // Assert: Fields should survive round-trip intact
            assertThat(response.getId()).isEqualTo(100L);
            assertThat(response.getFirstName()).isEqualTo("Ivan");
            assertThat(response.getLastName()).isEqualTo("Jackson");
            assertThat(response.getEmail()).isEqualTo("ivan@example.com");
            assertThat(response.getPhoneNumber()).isEqualTo("9876543210");
        }

        @Test
        @DisplayName("should support round-trip: UpdateRequest -> Entity -> Response")
        void testRoundTrip_UpdateRequestToResponse() {
            // Arrange: Start with an update request
            UpdateUserRequest updateRequest = UpdateUserRequest.builder()
                    .firstName("Karen")
                    .lastName("Lee")
                    .email("karen.lee@example.com")
                    .phoneNumber("8765432109")
                    .build();

            // Act: Map through layers
            User entity = userMapper.toEntity(updateRequest);
            entity.setId(50L);  // Set existing ID
            UserResponse response = userMapper.toResponse(entity);

            // Assert: All data should survive mapping cycle
            assertThat(response.getId()).isEqualTo(50L);
            assertThat(response.getFirstName()).isEqualTo("Karen");
            assertThat(response.getLastName()).isEqualTo("Lee");
            assertThat(response.getEmail()).isEqualTo("karen.lee@example.com");
            assertThat(response.getPhoneNumber()).isEqualTo("8765432109");
        }

        @Test
        @DisplayName("should ensure create and update request mapping produces identical field values")
        void testConsistency_CreateAndUpdateRequestMapping() {
            // Arrange: Same data in both request types
            String firstName = "Louis";
            String lastName = "Martin";
            String email = "louis@example.com";
            String phoneNumber = "9123456789";

            CreateUserRequest createRequest = CreateUserRequest.builder()
                    .firstName(firstName)
                    .lastName(lastName)
                    .email(email)
                    .phoneNumber(phoneNumber)
                    .build();

            UpdateUserRequest updateRequest = UpdateUserRequest.builder()
                    .firstName(firstName)
                    .lastName(lastName)
                    .email(email)
                    .phoneNumber(phoneNumber)
                    .build();

            // Act: Map both through separate mappers
            User entityFromCreate = userMapper.toEntity(createRequest);
            User entityFromUpdate = userMapper.toEntity(updateRequest);

            // Assert: Field values should be identical (both have ID as null)
            assertThat(entityFromCreate)
                    .usingRecursiveComparison()
                    .isEqualTo(entityFromUpdate);
        }
    }

    /**
     * Null safety and edge case tests.
     * Verify mapper behavior with unusual but possible input values.
     */
    @Nested
    @DisplayName("Null Safety and Edge Cases")
    class NullSafetyTests {

        @Test
        @DisplayName("should handle completely null User entity gracefully")
        void testToResponse_NullUserEntity() {
            // This test documents whether the mapper checks for null inputs
            // In production, null entities would cause NullPointerException
            // Best practice: validate at layer boundary before calling mapper

            // Act & Assert: Mapping null should fail predictably
            User nullUser = null;
            assertThatThrownBy(() -> userMapper.toResponse(nullUser))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("should handle single character names")
        void testToResponse_SingleCharacterNames() {
            // Arrange: Edge case - minimum valid name length
            User user = User.builder()
                    .id(3L)
                    .firstName("A")
                    .lastName("B")
                    .email("a.b@example.com")
                    .phoneNumber("1234567890")
                    .build();

            // Act
            UserResponse response = userMapper.toResponse(user);

            // Assert: Single character should be valid
            assertThat(response.getFirstName()).isEqualTo("A");
            assertThat(response.getLastName()).isEqualTo("B");
        }

        @Test
        @DisplayName("should handle unicode characters in names")
        void testToResponse_UnicodeCharactersInNames() {
            // Arrange: Names with unicode/international characters
            User user = User.builder()
                    .id(8L)
                    .firstName("José")
                    .lastName("Müller")
                    .email("jose.muller@example.com")
                    .phoneNumber("9876543210")
                    .build();

            // Act
            UserResponse response = userMapper.toResponse(user);

            // Assert: Unicode characters should preserve correctly
            assertThat(response.getFirstName()).isEqualTo("José");
            assertThat(response.getLastName()).isEqualTo("Müller");
        }
    }
}
