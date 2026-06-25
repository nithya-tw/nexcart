package com.nexcart.userservice;

import com.nexcart.userservice.exception.EmailAlreadyExistsException;
import com.nexcart.userservice.exception.UserNotFoundException;
import com.nexcart.userservice.user.controller.UserController;
import com.nexcart.userservice.user.dto.response.UserResponse;
import com.nexcart.userservice.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@ActiveProfiles("test")
@DisplayName("UserController Tests")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    private String createUserPayload;
    private String updateUserPayload;
    private UserResponse mockUser;

    @BeforeEach
    void setUp() {
        createUserPayload = """
                {
                    "firstName": "John",
                    "lastName": "Doe",
                    "email": "john.doe@example.com",
                    "phoneNumber": "1234567890"
                }
                """;

        updateUserPayload = """
                {
                    "firstName": "Jane",
                    "lastName": "Smith",
                    "email": "jane.smith@example.com",
                    "phoneNumber": "9876543210"
                }
                """;

        mockUser = UserResponse.builder()
            .id(1L)
            .firstName("John")
            .lastName("Doe")
            .email("john.doe@example.com")
            .phoneNumber("1234567890")
            .build();
    }

    @Test
    @DisplayName("POST /api/v1/users - Should create a new user successfully")
    void testCreateUserSuccess() throws Exception {
        when(userService.createUser(any())).thenReturn(mockUser);

        mockMvc.perform(post("/api/v1/users")
                .contentType(APPLICATION_JSON)
                .content(createUserPayload))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.firstName").value("John"))
            .andExpect(jsonPath("$.lastName").value("Doe"))
            .andExpect(jsonPath("$.email").value("john.doe@example.com"))
            .andExpect(jsonPath("$.phoneNumber").value("1234567890"));
    }

    @Test
    @DisplayName("POST /api/v1/users - Should return 400 when creating user with invalid data")
    void testCreateUserWithInvalidData() throws Exception {

        String invalidPayload = """
                {
                    "firstName": "",
                    "email": "invalid-email"
                }
                """;

        mockMvc.perform(post("/api/v1/users")
                .contentType(APPLICATION_JSON)
                .content(invalidPayload))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/users - Should return 409 when email already exists")
    void testCreateUserWithDuplicateEmail() throws Exception {

        when(userService.createUser(any()))
            .thenThrow(new EmailAlreadyExistsException("john.doe@example.com"));

        mockMvc.perform(post("/api/v1/users")
                .contentType(APPLICATION_JSON)
                .content(createUserPayload))
            .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("GET /api/v1/users/{id} - Should retrieve user by ID successfully")
    void testGetUserByIdSuccess() throws Exception {

        when(userService.getUser(1L)).thenReturn(mockUser);

        mockMvc.perform(get("/api/v1/users/{id}", 1)
                .contentType(APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.firstName").value("John"))
            .andExpect(jsonPath("$.lastName").value("Doe"))
            .andExpect(jsonPath("$.email").value("john.doe@example.com"))
            .andExpect(jsonPath("$.phoneNumber").value("1234567890"));
    }

    @Test
    @DisplayName("GET /api/v1/users/{id} - Should return 404 when user not found")
    void testGetUserByIdNotFound() throws Exception {

        when(userService.getUser(999L))
            .thenThrow(new UserNotFoundException(999L));

        mockMvc.perform(get("/api/v1/users/{id}", 999)
                .contentType(APPLICATION_JSON))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/v1/users - Should retrieve all users successfully")
    void testGetAllUsersSuccess() throws Exception {

        UserResponse user1 = UserResponse.builder()
            .id(1L)
            .firstName("John")
            .lastName("Doe")
            .email("john.doe@example.com")
            .phoneNumber("1234567890")
            .build();

        UserResponse user2 = UserResponse.builder()
            .id(2L)
            .firstName("Jane")
            .lastName("Smith")
            .email("jane.smith@example.com")
            .phoneNumber("9876543210")
            .build();

        List<UserResponse> users = Arrays.asList(user1, user2);

        when(userService.getUsers()).thenReturn(users);

        mockMvc.perform(get("/api/v1/users")
                .contentType(APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].id").value(1))
            .andExpect(jsonPath("$[0].firstName").value("John"))
            .andExpect(jsonPath("$[1].id").value(2))
            .andExpect(jsonPath("$[1].firstName").value("Jane"));
    }

    @Test
    @DisplayName("GET /api/v1/users - Should return empty list when no users exist")
    void testGetAllUsersEmpty() throws Exception {

        when(userService.getUsers()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/users")
                .contentType(APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("PUT /api/v1/users/{id} - Should update user successfully")
    void testUpdateUserSuccess() throws Exception {

        UserResponse updatedUser = UserResponse.builder()
            .id(1L)
            .firstName("Jane")
            .lastName("Smith")
            .email("jane.smith@example.com")
            .phoneNumber("9876543210")
            .build();

        when(userService.updateUser(eq(1L), any())).thenReturn(updatedUser);

        mockMvc.perform(put("/api/v1/users/{id}", 1)
                .contentType(APPLICATION_JSON)
                .content(updateUserPayload))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.firstName").value("Jane"))
            .andExpect(jsonPath("$.lastName").value("Smith"))
            .andExpect(jsonPath("$.email").value("jane.smith@example.com"))
            .andExpect(jsonPath("$.phoneNumber").value("9876543210"));
    }

    @Test
    @DisplayName("PUT /api/v1/users/{id} - Should return 404 when updating non-existent user")
    void testUpdateUserNotFound() throws Exception {

        when(userService.updateUser(eq(999L), any()))
            .thenThrow(new UserNotFoundException(999L));

        mockMvc.perform(put("/api/v1/users/{id}", 999)
                .contentType(APPLICATION_JSON)
                .content(updateUserPayload))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /api/v1/users/{id} - Should return 409 when email already exists")
    void testUpdateUserWithDuplicateEmail() throws Exception {

        when(userService.updateUser(eq(1L), any()))
            .thenThrow(new EmailAlreadyExistsException("john.doe@example.com"));

        mockMvc.perform(put("/api/v1/users/{id}", 1)
                .contentType(APPLICATION_JSON)
                .content(updateUserPayload))
            .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("PUT /api/v1/users/{id} - Should return 400 when update data is invalid")
    void testUpdateUserWithInvalidData() throws Exception {

        String invalidPayload = """
                {
                    "firstName": "",
                    "email": "invalid-email"
                }
                """;

        mockMvc.perform(put("/api/v1/users/{id}", 1)
                .contentType(APPLICATION_JSON)
                .content(invalidPayload))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("DELETE /api/v1/users/{id} - Should delete user successfully")
    void testDeleteUserSuccess() throws Exception {

        doNothing().when(userService).deleteUser(1L);

        mockMvc.perform(delete("/api/v1/users/{id}", 1)
                .contentType(APPLICATION_JSON))
            .andExpect(status().isNoContent());

        verify(userService, times(1)).deleteUser(1L);
    }

    @Test
    @DisplayName("DELETE /api/v1/users/{id} - Should return 404 when deleting non-existent user")
    void testDeleteUserNotFound() throws Exception {

        doThrow(new UserNotFoundException(999L))
            .when(userService).deleteUser(999L);

        mockMvc.perform(delete("/api/v1/users/{id}", 999)
                .contentType(APPLICATION_JSON))
            .andExpect(status().isNotFound());
    }
}
