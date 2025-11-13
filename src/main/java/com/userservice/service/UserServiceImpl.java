package com.userservice.service;

import com.userservice.dto.UserCreateDTO;
import com.userservice.dto.UserResponseDTO;
import com.userservice.dto.UserUpdateDTO;
import com.userservice.entity.UserEntity;
import com.userservice.exception.DuplicateResourceException;
import com.userservice.exception.ResourceNotFoundException;
import com.userservice.mapper.UserMapper;
import com.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Реализация UserService, предоставляющая бизнес-логику для управления пользователями.
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    @Transactional
    public UserResponseDTO createUser(UserCreateDTO request) {
        log.debug("Creating user with email: {}", request.getEmail());

        // Проверка уникальности email
        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Attempt to create user with existing email: {}", request.getEmail());
            throw new DuplicateResourceException("User with email " + request.getEmail() + " already exists");
        }

        UserEntity createdUser = userMapper.ofDTO(request);
        UserEntity savedUser = userRepository.save(createdUser);

        log.info("User created successfully with id: {}", savedUser.getId());

        return userMapper.ofEntity(savedUser);
    }

    @Override
    public UserResponseDTO getUserById(Long id) {
        log.debug("Fetching user by id: {}", id);

        UserEntity userEntity = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        return userMapper.ofEntity(userEntity);
    }

    @Override
    public List<UserResponseDTO> getAllUsers() {
        log.debug("Fetching all users");

        return userRepository.findAll().stream()
                .map(userMapper::ofEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public UserResponseDTO updateUser(Long id, UserUpdateDTO request) {
        log.debug("Updating user with id: {}", id);

        UserEntity existingUser = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        // Проверка уникальности email, если такой уже есть в базе
        if (request.getEmail() != null && !existingUser.getEmail().equals(request.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                log.warn("Attempt to update user with existing email: {}", request.getEmail());
                throw new DuplicateResourceException("User with email " + request.getEmail() + " already exists");
            }
        }

        userMapper.updateEntityFromDTO(request, existingUser);
        UserEntity updatedUser = userRepository.save(existingUser);

        log.info("User updated successfully with id: {}", id);
        return userMapper.ofEntity(updatedUser);
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        log.debug("Deleting user with id: {}", id);

        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        userRepository.delete(user);
        log.info("User deleted successfully with id: {}", id);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }
}