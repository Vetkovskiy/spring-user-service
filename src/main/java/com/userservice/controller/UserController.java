package com.userservice.controller;

import com.userservice.dto.UserCreateDTO;
import com.userservice.dto.UserResponseDTO;
import com.userservice.dto.UserUpdateDTO;
import com.userservice.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST-контроллер для управления пользователями.
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;

    /**
     * Создать нового пользователя
     * POST /api/users
     */
    @PostMapping
    public ResponseEntity<UserResponseDTO> createUser(
            @Valid @RequestBody UserCreateDTO request) {

        log.debug("REST request to create user: {}", request.getEmail());
        UserResponseDTO created = userService.createUser(request);

        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    /**
     * Получить пользователя по ID
     * GET /api/users/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDTO> getUserById(
            @PathVariable Long id) {

        log.debug("REST request to get user by id: {}", id);
        UserResponseDTO user = userService.getUserById(id);

        return ResponseEntity.ok(user);
    }

    /**
     * Получить всех пользователей
     * GET /api/users
     */
    @GetMapping
    public ResponseEntity<List<UserResponseDTO>> getAllUsers() {

        log.debug("REST request to get all users");
        List<UserResponseDTO> users = userService.getAllUsers();

        return ResponseEntity.ok(users);
    }

    /**
     * Обновить пользователя
     * PUT /api/users/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<UserResponseDTO> updateUser(@PathVariable Long id,
            @Valid @RequestBody UserUpdateDTO request) {

        log.debug("REST request to update user with id: {}", id);
        UserResponseDTO updated = userService.updateUser(id, request);

        return ResponseEntity.ok(updated);
    }

    /**
     * Удалить пользователя
     * DELETE /api/users/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(
            @PathVariable Long id) {

        log.debug("REST request to delete user with id: {}", id);
        userService.deleteUser(id);

        return ResponseEntity.noContent().build();
    }

    /**
     * Проверить существование email
     * GET /api/users/exists?email=test@example.com
     */
    @GetMapping("/exists")
    public ResponseEntity<Boolean> existsByEmail(
            @RequestParam String email) {

        log.debug("REST request to check if email exists: {}", email);
        boolean exists = userService.existsByEmail(email);

        return ResponseEntity.ok(exists);
    }
}
