package com.userservice.service;

import com.userservice.dto.UserCreateDTO;
import com.userservice.dto.UserResponseDTO;
import com.userservice.dto.UserUpdateDTO;

import java.util.List;
/**
 * Сервисный интерфейс для управления пользователями.
 */
public interface UserService {
    UserResponseDTO createUser(UserCreateDTO request);
    UserResponseDTO getUserById(Long id);
    List<UserResponseDTO> getAllUsers();
    UserResponseDTO updateUser(Long id, UserUpdateDTO request);
    void deleteUser(Long id);
    boolean existsByEmail(String email);
}