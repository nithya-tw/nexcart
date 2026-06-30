package com.nexcart.userservice.user.service;

import com.nexcart.userservice.exception.EmailAlreadyExistsException;
import com.nexcart.userservice.exception.UserNotFoundException;
import com.nexcart.userservice.user.dto.request.CreateUserRequest;
import com.nexcart.userservice.user.dto.request.UpdateUserRequest;
import com.nexcart.userservice.user.dto.response.UserResponse;
import com.nexcart.userservice.user.entity.User;
import com.nexcart.userservice.user.mapper.UserMapper;
import com.nexcart.userservice.user.repository.UserRepository;
import com.nexcart.userservice.user.service.impl.JpaUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for JpaUserService.
 * Tests user management operations including create, update, delete, and search.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JpaUserService Unit Tests")
class JpaUserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private JpaUserService userService;

    private CreateUserRequest createRequest;
    private UpdateUserRequest updateRequest;
    private User mockUser;
    private UserResponse mockResponse;

    @BeforeEach
    void setUp() {
        createRequest = new CreateUserRequest(
                "John",
                "Doe",
                "john.doe@example.com",
                "1234567890"
        );

        updateRequest = new UpdateUserRequest(
                "Jane",
                "Doe",
                "jane.doe@example.com",
                "0987654321"
        );

        mockUser = User.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .phoneNumber("1234567890")
                .build();

        mockResponse = UserResponse.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .phoneNumber("1234567890")
                .build();
    }

    @Test
    @DisplayName("Should create user successfully with valid data")
    void shouldCreateUserSuccessfully() {
        when(userRepository.findByEmail(createRequest.getEmail())).thenReturn(Optional.empty());
        when(userMapper.toEntity(createRequest)).thenReturn(mockUser);
        when(userRepository.save(any(User.class))).thenReturn(mockUser);
        when(userMapper.toResponse(mockUser)).thenReturn(mockResponse);

        UserResponse result = userService.createUser(createRequest);

        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo(createRequest.getEmail());
        verify(userRepository).findByEmail(createRequest.getEmail());
        verify(userRepository).save(any(User.class));
        verify(userMapper).toResponse(mockUser);
    }

    @Test
    @DisplayName("Should throw EmailAlreadyExistsException when email exists")
    void shouldThrowExceptionWhenEmailExists() {
        when(userRepository.findByEmail(createRequest.getEmail()))
                .thenReturn(Optional.of(mockUser));

        assertThatThrownBy(() -> userService.createUser(createRequest))
                .isInstanceOf(EmailAlreadyExistsException.class)
                .hasMessageContaining(createRequest.getEmail());

        verify(userRepository).findByEmail(createRequest.getEmail());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should get user by ID successfully")
    void shouldGetUserById() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(userMapper.toResponse(mockUser)).thenReturn(mockResponse);

        UserResponse result = userService.getUser(1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        verify(userRepository).findById(1L);
    }

    @Test
    @DisplayName("Should throw UserNotFoundException when user not found")
    void shouldThrowExceptionWhenUserNotFound() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUser(999L))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("999");

        verify(userRepository).findById(999L);
    }

    @Test
    @DisplayName("Should get all users successfully")
    void shouldGetAllUsers() {
        User user2 = User.builder()
                .id(2L)
                .firstName("Jane")
                .lastName("Smith")
                .email("jane.smith@example.com")
                .phoneNumber("9876543210")
                .build();

        UserResponse response2 = UserResponse.builder()
                .id(2L)
                .firstName("Jane")
                .lastName("Smith")
                .email("jane.smith@example.com")
                .phoneNumber("9876543210")
                .build();

        when(userRepository.findAll()).thenReturn(Arrays.asList(mockUser, user2));
        when(userMapper.toResponse(mockUser)).thenReturn(mockResponse);
        when(userMapper.toResponse(user2)).thenReturn(response2);

        List<UserResponse> result = userService.getUsers();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getEmail()).isEqualTo(mockUser.getEmail());
        assertThat(result.get(1).getEmail()).isEqualTo(user2.getEmail());
        verify(userRepository).findAll();
    }

    @Test
    @DisplayName("Should update user successfully")
    void shouldUpdateUserSuccessfully() {
        User updatedUser = User.builder()
                .id(1L)
                .firstName(updateRequest.getFirstName())
                .lastName(updateRequest.getLastName())
                .email(updateRequest.getEmail())
                .phoneNumber(updateRequest.getPhoneNumber())
                .build();

        UserResponse updatedResponse = UserResponse.builder()
                .id(1L)
                .firstName(updateRequest.getFirstName())
                .lastName(updateRequest.getLastName())
                .email(updateRequest.getEmail())
                .phoneNumber(updateRequest.getPhoneNumber())
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(userRepository.findByEmail(updateRequest.getEmail())).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(updatedUser);
        when(userMapper.toResponse(updatedUser)).thenReturn(updatedResponse);

        UserResponse result = userService.updateUser(1L, updateRequest);

        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo(updateRequest.getEmail());
        verify(userRepository).findById(1L);
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw exception when updating with existing email")
    void shouldThrowExceptionWhenUpdatingWithExistingEmail() {
        User anotherUser = User.builder()
                .id(2L)
                .email(updateRequest.getEmail())
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(userRepository.findByEmail(updateRequest.getEmail()))
                .thenReturn(Optional.of(anotherUser));

        assertThatThrownBy(() -> userService.updateUser(1L, updateRequest))
                .isInstanceOf(EmailAlreadyExistsException.class);

        verify(userRepository).findById(1L);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should allow updating with same email")
    void shouldAllowUpdatingWithSameEmail() {
        UpdateUserRequest sameEmailRequest = new UpdateUserRequest(
                "John Updated",
                "Doe Updated",
                mockUser.getEmail(),
                "9999999999"
        );

        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(userRepository.save(any(User.class))).thenReturn(mockUser);
        when(userMapper.toResponse(any(User.class))).thenReturn(mockResponse);

        UserResponse result = userService.updateUser(1L, sameEmailRequest);

        assertThat(result).isNotNull();
        verify(userRepository).findById(1L);
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should delete user successfully")
    void shouldDeleteUserSuccessfully() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));

        userService.deleteUser(1L);

        verify(userRepository).findById(1L);
        verify(userRepository).delete(mockUser);
    }

    @Test
    @DisplayName("Should throw exception when deleting non-existent user")
    void shouldThrowExceptionWhenDeletingNonExistentUser() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.deleteUser(999L))
                .isInstanceOf(UserNotFoundException.class);

        verify(userRepository).findById(999L);
        verify(userRepository, never()).delete(any(User.class));
    }

    @Test
    @DisplayName("Should search users by email successfully")
    void shouldSearchUsersByEmail() {
        User user2 = User.builder()
                .id(2L)
                .firstName("Johnny")
                .lastName("Doe")
                .email("johnny.doe@example.com")
                .phoneNumber("1111111111")
                .build();

        UserResponse response2 = UserResponse.builder()
                .id(2L)
                .firstName("Johnny")
                .lastName("Doe")
                .email("johnny.doe@example.com")
                .phoneNumber("1111111111")
                .build();

        when(userRepository.findAll()).thenReturn(Arrays.asList(mockUser, user2));
        when(userMapper.toResponse(mockUser)).thenReturn(mockResponse);
        when(userMapper.toResponse(user2)).thenReturn(response2);

        List<UserResponse> result = userService.searchUsersByEmail("doe");

        assertThat(result).hasSize(2);
        assertThat(result).allMatch(r -> r.getEmail().contains("doe"));
        verify(userRepository).findAll();
    }

    @Test
    @DisplayName("Should return empty list when no users match email search")
    void shouldReturnEmptyListWhenNoUsersMatch() {
        when(userRepository.findAll()).thenReturn(Arrays.asList(mockUser));

        List<UserResponse> result = userService.searchUsersByEmail("nonexistent");

        assertThat(result).isEmpty();
        verify(userRepository).findAll();
    }
}
