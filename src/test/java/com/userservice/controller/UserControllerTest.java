package com.userservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.userservice.dto.UserCreateDTO;
import com.userservice.dto.UserResponseDTO;
import com.userservice.dto.UserUpdateDTO;
import com.userservice.exception.DuplicateResourceException;
import com.userservice.exception.ResourceNotFoundException;
import com.userservice.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@DisplayName("UserController Unit Tests")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    private UserCreateDTO createDTO;
    private UserUpdateDTO updateDTO;
    private UserResponseDTO responseDTO;
    private LocalDateTime testTime;

    @BeforeEach
    void setUp() {
        testTime = LocalDateTime.of(2024, 1, 1, 12, 0);

        createDTO = UserCreateDTO.builder()
                .name("John Doe")
                .email("john@example.com")
                .age(30)
                .build();

        updateDTO = new UserUpdateDTO("Jane Doe", "jane@example.com", 25);

        responseDTO = UserResponseDTO.builder()
                .id(1L)
                .name("John Doe")
                .email("john@example.com")
                .age(30)
                .createdAt(testTime)
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/users - успешное создание пользователя")
    void createUser_Success() throws Exception {
        when(userService.createUser(any(UserCreateDTO.class))).thenReturn(responseDTO);

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.name", is("John Doe")))
                .andExpect(jsonPath("$.email", is("john@example.com")))
                .andExpect(jsonPath("$.age", is(30)));

        verify(userService).createUser(any(UserCreateDTO.class));
    }

    @Test
    @DisplayName("POST /api/v1/users - валидация: пустое имя")
    void createUser_ValidationFailure_EmptyName() throws Exception {
        createDTO = UserCreateDTO.builder().name("").build();

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Validation Failed")));

        verify(userService, never()).createUser(any());
    }

    @Test
    @DisplayName("POST /api/v1/users - валидация: невалидный email")
    void createUser_ValidationFailure_InvalidEmail() throws Exception {
        createDTO = UserCreateDTO.builder().email("invalid-email").build();

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").exists());

        verify(userService, never()).createUser(any());
    }

    @Test
    @DisplayName("POST /api/v1/users - валидация: возраст меньше 0")
    void createUser_ValidationFailure_NegativeAge() throws Exception {
        createDTO = UserCreateDTO.builder().age(-1).build();

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.age").exists());

        verify(userService, never()).createUser(any());
    }

    @Test
    @DisplayName("POST /api/v1/users - валидация: возраст больше 150")
    void createUser_ValidationFailure_AgeTooHigh() throws Exception {
        createDTO = UserCreateDTO.builder().age(151).build();

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.age").exists());

        verify(userService, never()).createUser(any());
    }

    @Test
    @DisplayName("POST /api/v1/users - конфликт при дублировании email")
    void createUser_Conflict_DuplicateEmail() throws Exception {
        when(userService.createUser(any(UserCreateDTO.class)))
                .thenThrow(new DuplicateResourceException("User with email john@example.com already exists"));

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.error", is("Conflict")))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("already exists")));

        verify(userService).createUser(any(UserCreateDTO.class));
    }

    @Test
    @DisplayName("POST /api/v1/users - malformed JSON")
    void createUser_MalformedJson() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid json}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Bad Request")))
                .andExpect(jsonPath("$.message", is("Malformed JSON request")));

        verify(userService, never()).createUser(any());
    }

    @Test
    @DisplayName("GET /api/v1/users/{id} - успешное получение пользователя")
    void getUserById_Success() throws Exception {
        when(userService.getUserById(1L)).thenReturn(responseDTO);

        mockMvc.perform(get("/api/v1/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.name", is("John Doe")))
                .andExpect(jsonPath("$.email", is("john@example.com")));

        verify(userService).getUserById(1L);
    }

    @Test
    @DisplayName("GET /api/v1/users/{id} - пользователь не найден")
    void getUserById_NotFound() throws Exception {
        when(userService.getUserById(999L))
                .thenThrow(new ResourceNotFoundException("User not found with id: 999"));

        mockMvc.perform(get("/api/v1/users/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.error", is("Not Found")))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("not found")));

        verify(userService).getUserById(999L);
    }

    @Test
    @DisplayName("GET /api/v1/users/{id} - невалидный тип ID")
    void getUserById_InvalidIdType() throws Exception {
        mockMvc.perform(get("/api/v1/users/invalid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Bad Request")));

        verify(userService, never()).getUserById(any());
    }

    @Test
    @DisplayName("GET /api/v1/users - успешное получение всех пользователей")
    void getAllUsers_Success() throws Exception {
        UserResponseDTO user2 = UserResponseDTO.builder()
                .id(2L)
                .name("Jane Smith")
                .email("jane@example.com")
                .age(25)
                .createdAt(testTime)
                .build();

        when(userService.getAllUsers()).thenReturn(List.of(responseDTO, user2));

        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[1].id", is(2)));

        verify(userService).getAllUsers();
    }

    @Test
    @DisplayName("GET /api/v1/users - пустой список")
    void getAllUsers_EmptyList() throws Exception {
        when(userService.getAllUsers()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        verify(userService).getAllUsers();
    }

    @Test
    @DisplayName("PUT /api/v1/users/{id} - успешное обновление")
    void updateUser_Success() throws Exception {
        UserResponseDTO updatedResponse = UserResponseDTO.builder()
                .id(1L)
                .name("Jane Doe")
                .email("jane@example.com")
                .age(25)
                .createdAt(testTime)
                .build();

        when(userService.updateUser(eq(1L), any(UserUpdateDTO.class))).thenReturn(updatedResponse);

        mockMvc.perform(put("/api/v1/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.name", is("Jane Doe")))
                .andExpect(jsonPath("$.email", is("jane@example.com")));

        verify(userService).updateUser(eq(1L), any(UserUpdateDTO.class));
    }

    @Test
    @DisplayName("PUT /api/v1/users/{id} - пользователь не найден")
    void updateUser_NotFound() throws Exception {
        when(userService.updateUser(eq(999L), any(UserUpdateDTO.class)))
                .thenThrow(new ResourceNotFoundException("User not found with id: 999"));

        mockMvc.perform(put("/api/v1/users/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)));

        verify(userService).updateUser(eq(999L), any(UserUpdateDTO.class));
    }

    @Test
    @DisplayName("PUT /api/v1/users/{id} - конфликт при дублировании email")
    void updateUser_Conflict_DuplicateEmail() throws Exception {
        when(userService.updateUser(eq(1L), any(UserUpdateDTO.class)))
                .thenThrow(new DuplicateResourceException("User with email jane@example.com already exists"));

        mockMvc.perform(put("/api/v1/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)));

        verify(userService).updateUser(eq(1L), any(UserUpdateDTO.class));
    }

    @Test
    @DisplayName("PUT /api/v1/users/{id} - валидация невалидного email")
    void updateUser_ValidationFailure_InvalidEmail() throws Exception {
        updateDTO.setEmail("invalid-email");

        mockMvc.perform(put("/api/v1/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").exists());

        verify(userService, never()).updateUser(any(), any());
    }

    @Test
    @DisplayName("DELETE /api/v1/users/{id} - успешное удаление")
    void deleteUser_Success() throws Exception {
        doNothing().when(userService).deleteUser(1L);

        mockMvc.perform(delete("/api/v1/users/1"))
                .andExpect(status().isNoContent());

        verify(userService).deleteUser(1L);
    }

    @Test
    @DisplayName("DELETE /api/v1/users/{id} - пользователь не найден")
    void deleteUser_NotFound() throws Exception {
        doThrow(new ResourceNotFoundException("User not found with id: 999"))
                .when(userService).deleteUser(999L);

        mockMvc.perform(delete("/api/v1/users/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)));

        verify(userService).deleteUser(999L);
    }

    @Test
    @DisplayName("GET /api/v1/users/exists - email существует")
    void existsByEmail_ReturnsTrue() throws Exception {
        when(userService.existsByEmail("test@example.com")).thenReturn(true);

        mockMvc.perform(get("/api/v1/users/exists")
                        .param("email", "test@example.com"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        verify(userService).existsByEmail("test@example.com");
    }

    @Test
    @DisplayName("GET /api/v1/users/exists - email не существует")
    void existsByEmail_ReturnsFalse() throws Exception {
        when(userService.existsByEmail("nonexistent@example.com")).thenReturn(false);

        mockMvc.perform(get("/api/v1/users/exists")
                        .param("email", "nonexistent@example.com"))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));

        verify(userService).existsByEmail("nonexistent@example.com");
    }

    @Test
    @DisplayName("GET /api/v1/users/exists - отсутствует обязательный параметр email")
    void existsByEmail_MissingParameter() throws Exception {
        mockMvc.perform(get("/api/v1/users/exists"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Bad Request")))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("email")));

        verify(userService, never()).existsByEmail(any());
    }
}
