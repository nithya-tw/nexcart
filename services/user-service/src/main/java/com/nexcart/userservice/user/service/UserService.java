package com.nexcart.userservice.user.service;

import com.nexcart.userservice.user.dto.request.CreateUserRequest;
import com.nexcart.userservice.user.dto.request.UpdateUserRequest;
import com.nexcart.userservice.user.dto.response.UserResponse;

import java.util.List;

public interface UserService {

    UserResponse createUser(CreateUserRequest request);

    UserResponse getUser(Long id);

    List<UserResponse> getUsers();

    UserResponse updateUser(Long id, UpdateUserRequest request);

    void deleteUser(Long id);

    List<UserResponse> searchUsersByEmail(String email);
}
