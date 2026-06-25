package com.nexcart.userservice.user.service.impl;

import com.nexcart.userservice.exception.EmailAlreadyExistsException;
import com.nexcart.userservice.exception.UserNotFoundException;
import com.nexcart.userservice.user.dto.request.CreateUserRequest;
import com.nexcart.userservice.user.dto.request.UpdateUserRequest;
import com.nexcart.userservice.user.dto.response.UserResponse;
import com.nexcart.userservice.user.entity.User;
import com.nexcart.userservice.user.mapper.UserMapper;
import com.nexcart.userservice.user.repository.UserRepository;
import com.nexcart.userservice.user.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class JpaUserService implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public JpaUserService(UserRepository userRepository, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }

    @Override
    public UserResponse createUser(CreateUserRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new EmailAlreadyExistsException(request.getEmail());
        }

        User user = userMapper.toEntity(request);
        User savedUser = userRepository.save(user);
        return userMapper.toResponse(savedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUser(Long id) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new UserNotFoundException(id));
        return userMapper.toResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getUsers() {
        return userRepository.findAll().stream()
                .map(userMapper::toResponse)
                .toList();
    }

    @Override
    public UserResponse updateUser(Long id, UpdateUserRequest request) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new UserNotFoundException(id));

        if (!user.getEmail().equals(request.getEmail()) &&
            userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new EmailAlreadyExistsException(request.getEmail());
        }

        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setPhoneNumber(request.getPhoneNumber());

        User updatedUser = userRepository.save(user);
        return userMapper.toResponse(updatedUser);
    }

    @Override
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new UserNotFoundException(id));
        userRepository.delete(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> searchUsersByEmail(String email) {
        return userRepository.findAll().stream()
                .filter(user -> user.getEmail().contains(email))
                .map(userMapper::toResponse)
                .toList();
    }
}
