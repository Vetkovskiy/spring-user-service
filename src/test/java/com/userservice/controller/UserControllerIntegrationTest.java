package com.userservice.controller;

import com.userservice.base.BaseIntegrationTest;
import com.userservice.dto.UserCreateDTO;
import com.userservice.dto.UserResponseDTO;
import com.userservice.dto.UserUpdateDTO;
import com.userservice.entity.UserEntity;
import com.userservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("UserController Integration Tests")
class UserControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    private static final String BASE_URL = "/api/v1/users";

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Полный CRUD цикл пользователя")
    void fullUserLifecycle() {
        //создание пользователя
        UserCreateDTO createRequest = UserCreateDTO.builder()
                .name("John Doe")
                .email("john@example.com")
                .age(30)
                .build();

        ResponseEntity<UserResponseDTO> createResponse = restTemplate.postForEntity(
                BASE_URL,
                createRequest,
                UserResponseDTO.class
        );

        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(createResponse.getBody()).isNotNull();
        assertThat(createResponse.getBody().getId()).isNotNull();
        assertThat(createResponse.getBody().getName()).isEqualTo("John Doe");
        assertThat(createResponse.getBody().getEmail()).isEqualTo("john@example.com");

        Long userId = createResponse.getBody().getId();

        //получение пользователя по ID
        ResponseEntity<UserResponseDTO> getResponse = restTemplate.getForEntity(
                BASE_URL + "/" + userId,
                UserResponseDTO.class
        );

        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getResponse.getBody()).isNotNull();
        assertThat(getResponse.getBody().getId()).isEqualTo(userId);

        //обновление пользователя
        UserUpdateDTO updateRequest = new UserUpdateDTO();
        updateRequest.setName("Jane Doe");
        updateRequest.setAge(25);


        ResponseEntity<UserResponseDTO> updateResponse = restTemplate.exchange(
                BASE_URL + "/" + userId,
                HttpMethod.PUT,
                new HttpEntity<>(updateRequest),
                UserResponseDTO.class
        );

        assertThat(updateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updateResponse.getBody()).isNotNull();
        assertThat(updateResponse.getBody().getName()).isEqualTo("Jane Doe");
        assertThat(updateResponse.getBody().getAge()).isEqualTo(25);
        assertThat(updateResponse.getBody().getEmail()).isEqualTo("john@example.com"); // email не изменился

        //удаление пользователя
        ResponseEntity<Void> deleteResponse = restTemplate.exchange(
                BASE_URL + "/" + userId,
                HttpMethod.DELETE,
                null,
                Void.class
        );

        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        //Проверка, что пользователь удален
        ResponseEntity<Map<String, Object>> notFoundResponse = restTemplate.exchange(
                BASE_URL + "/" + userId,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {
                }
        );

        assertThat(notFoundResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("Создание пользователя с дублирующим email возвращает 409 Conflict")
    void createUser_DuplicateEmail_ReturnsConflict() {
        UserCreateDTO firstUser = UserCreateDTO.builder()
                .name("First User")
                .email("duplicate@example.com")
                .age(25)
                .build();

        restTemplate.postForEntity(BASE_URL, firstUser, UserResponseDTO.class);

        UserCreateDTO secondUser = UserCreateDTO.builder()
                .name("Second User")
                .email("duplicate@example.com")
                .age(30)
                .build();

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                BASE_URL,
                HttpMethod.POST,
                new HttpEntity<>(secondUser),
                new ParameterizedTypeReference<>() {
                }
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("status")).isEqualTo(409);
        assertThat(response.getBody().get("message").toString()).contains("already exists");
    }

    @Test
    @DisplayName("Получение всех пользователей возвращает список")
    void getAllUsers_ReturnsListOfUsers() {
        createTestUser("User 1", "user1@example.com", 25);
        createTestUser("User 2", "user2@example.com", 30);
        createTestUser("User 3", "user3@example.com", 35);

        ResponseEntity<List<UserResponseDTO>> response = restTemplate.exchange(
                BASE_URL,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {
                }
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(3);
    }

    @Test
    @DisplayName("Обновление email на уже существующий возвращает 409 Conflict")
    void updateUser_DuplicateEmail_ReturnsConflict() {
        Long user1Id = createTestUser("User 1", "user1@example.com", 25);
        createTestUser("User 2", "user2@example.com", 30);

        UserUpdateDTO updateRequest = new UserUpdateDTO();
        updateRequest.setEmail("user2@example.com");


        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                BASE_URL + "/" + user1Id,
                HttpMethod.PUT,
                new HttpEntity<>(updateRequest),
                new ParameterizedTypeReference<>() {
                }
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("status")).isEqualTo(409);
    }

    @Test
    @DisplayName("Проверка существования email")
    void existsByEmail_EmailExists_ReturnsTrue() {
        createTestUser("Test User", "test@example.com", 25);

        ResponseEntity<Boolean> response = restTemplate.getForEntity(
                BASE_URL + "/exists?email=test@example.com",
                Boolean.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isTrue();
    }

    @Test
    @DisplayName("Обновление пользователя с null значениями не изменяет существующие поля")
    void updateUser_NullValues_DoesNotChangeExistingFields() {
        Long userId = createTestUser("Original Name", "original@example.com", 30);

        UserUpdateDTO updateRequest = new UserUpdateDTO("Updated Name", null, null);

        ResponseEntity<UserResponseDTO> response = restTemplate.exchange(
                BASE_URL + "/" + userId,
                HttpMethod.PUT,
                new HttpEntity<>(updateRequest),
                UserResponseDTO.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getName()).isEqualTo("Updated Name");
        assertThat(response.getBody().getEmail()).isEqualTo("original@example.com");
        assertThat(response.getBody().getAge()).isEqualTo(30);
    }

    @Test
    @DisplayName("CreatedAt устанавливается автоматически")
    void createUser_CreatedAtIsSetAutomatically() {
        UserCreateDTO createRequest = UserCreateDTO.builder()
                .name("Test User")
                .email("test@example.com")
                .age(25)
                .build();

        ResponseEntity<UserResponseDTO> response = restTemplate.postForEntity(
                BASE_URL,
                createRequest,
                UserResponseDTO.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCreatedAt()).isNotNull();
    }

    // Вспомогательный метод для создания тестовых пользователей
    private Long createTestUser(String name, String email, Integer age) {
        UserEntity user = new UserEntity();
        user.setName(name);
        user.setEmail(email);
        user.setAge(age);
        return userRepository.save(user).getId();
    }
}
