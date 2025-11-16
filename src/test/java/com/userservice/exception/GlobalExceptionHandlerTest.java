package com.userservice.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.userservice.controller.UserController;
import com.userservice.dto.UserCreateDTO;
import com.userservice.dto.UserUpdateDTO;
import com.userservice.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import(GlobalExceptionHandler.class)
@DisplayName("GlobalExceptionHandler Unit Tests")
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @Test
    @DisplayName("handleResourceNotFoundException: возврат 404 NOT_FOUND")
    void handleResourceNotFoundException_Returns404() throws Exception {
        when(userService.getUserById(999L))
                .thenThrow(new ResourceNotFoundException("User not found with id: 999"));

        mockMvc.perform(get("/api/v1/users/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.error", is("Not Found")))
                .andExpect(jsonPath("$.message", containsString("not found")))
                .andExpect(jsonPath("$.path", is("/api/v1/users/999")))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("handleDuplicateResourceException: возврат 409 CONFLICT")
    void handleDuplicateResourceException_Returns409() throws Exception {
        UserCreateDTO createDTO = UserCreateDTO.builder()
                .name("John Doe")
                .email("duplicate@example.com")
                .age(30)
                .build();

        when(userService.createUser(any(UserCreateDTO.class)))
                .thenThrow(new DuplicateResourceException("User with email duplicate@example.com already exists"));

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.error", is("Conflict")))
                .andExpect(jsonPath("$.message", containsString("already exists")))
                .andExpect(jsonPath("$.path", is("/api/v1/users")))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("handleValidationExceptions: возврат 400 BAD_REQUEST с деталями ошибок")
    void handleValidationExceptions_Returns400WithErrorDetails() throws Exception {
        UserCreateDTO invalidDTO = UserCreateDTO.builder()
                .name("")
                .email("invalid-email")
                .age(-5)
                .build();

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Validation Failed")))
                .andExpect(jsonPath("$.message", is("Invalid request parameters")))
                .andExpect(jsonPath("$.path", is("/api/v1/users")))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.errors").isMap())
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.age").exists());
    }

    @Test
    @DisplayName("handleValidationExceptions: валидация @NotBlank")
    void handleValidationExceptions_NotBlankValidation() throws Exception {
        UserCreateDTO invalidDTO = UserCreateDTO.builder()
                .name("   ") // только пробелы
                .email("test@example.com")
                .age(25)
                .build();

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists());
    }

    @Test
    @DisplayName("handleValidationExceptions: валидация @Email")
    void handleValidationExceptions_EmailValidation() throws Exception {
        UserCreateDTO invalidDTO = UserCreateDTO.builder()
                .name("John Doe")
                .email("not-an-email")
                .age(25)
                .build();

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").exists());
    }

    @Test
    @DisplayName("handleValidationExceptions: валидация @Min и @Max для возраста")
    void handleValidationExceptions_AgeConstraints() throws Exception {
        UserCreateDTO tooYoung = UserCreateDTO.builder()
                .name("John Doe")
                .email("test@example.com")
                .age(-1)
                .build();

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tooYoung)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.age").exists());

        UserCreateDTO tooOld = UserCreateDTO.builder()
                .name("John Doe")
                .email("test@example.com")
                .age(151)
                .build();

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tooOld)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.age").exists());
    }

    @Test
    @DisplayName("handleValidationExceptions: валидация @Size для имени")
    void handleValidationExceptions_NameSizeConstraints() throws Exception {
        String longName = "a".repeat(101);
        UserCreateDTO invalidDTO = UserCreateDTO.builder()
                .name(longName)
                .email("test@example.com")
                .age(25)
                .build();

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists());
    }

    @Test
    @DisplayName("handleValidationExceptions: валидация @Size для email")
    void handleValidationExceptions_EmailSizeConstraints() throws Exception {
        String longEmail = "a".repeat(140) + "@example.com";
        UserCreateDTO invalidDTO = UserCreateDTO.builder()
                .name("John Doe")
                .email(longEmail)
                .age(25)
                .build();

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").exists());
    }

    @Test
    @DisplayName("handleMissingParams: возврат 400 при отсутствии обязательного параметра")
    void handleMissingParams_Returns400() throws Exception {
        mockMvc.perform(get("/api/v1/users/exists"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Bad Request")))
                .andExpect(jsonPath("$.message", containsString("email")))
                .andExpect(jsonPath("$.path", is("/api/v1/users/exists")))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("handleTypeMismatch: возврат 400 при несоответствии типа параметра")
    void handleTypeMismatch_Returns400() throws Exception {
        mockMvc.perform(get("/api/v1/users/invalid-id"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Bad Request")))
                .andExpect(jsonPath("$.message", containsString("Invalid value")))
                .andExpect(jsonPath("$.path", is("/api/v1/users/invalid-id")))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("handleHttpMessageNotReadable: возврат 400 при невалидном JSON")
    void handleHttpMessageNotReadable_Returns400() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid json}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Bad Request")))
                .andExpect(jsonPath("$.message", is("Malformed JSON request")))
                .andExpect(jsonPath("$.path", is("/api/v1/users")))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("handleGlobalException: возврат 500 INTERNAL_SERVER_ERROR для непредвиденных ошибок")
    void handleGlobalException_Returns500() throws Exception {
        when(userService.getUserById(1L))
                .thenThrow(new RuntimeException("Unexpected error"));

        mockMvc.perform(get("/api/v1/users/1"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status", is(500)))
                .andExpect(jsonPath("$.error", is("Internal Server Error")))
                .andExpect(jsonPath("$.message", is("An unexpected error occurred")))
                .andExpect(jsonPath("$.path", is("/api/v1/users/1")))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("Multiple validation errors: все ошибки валидации возвращаются одновременно")
    void handleValidationExceptions_MultipleErrors() throws Exception {
        UserCreateDTO invalidDTO = UserCreateDTO.builder()
                .name("")
                .email("bad-email")
                .age(200)
                .build();

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isMap())
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.age").exists());
    }

    @Test
    @DisplayName("Update validation: валидация работает для PUT запросов")
    void handleValidationExceptions_ForUpdateRequests() throws Exception {
        UserUpdateDTO invalidDTO = new UserUpdateDTO("a".repeat(101), "invalid", -10);

        mockMvc.perform(put("/api/v1/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isMap())
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.age").exists());
    }

    @Test
    @DisplayName("Path variable type mismatch: невалидный тип для path variable")
    void handleTypeMismatch_PathVariable() throws Exception {
        mockMvc.perform(delete("/api/v1/users/not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Bad Request")))
                .andExpect(jsonPath("$.message", containsString("Invalid value")));
    }

    @Test
    @DisplayName("Duplicate resource on update: конфликт при обновлении")
    void handleDuplicateResourceException_OnUpdate_Returns409() throws Exception {
        UserUpdateDTO updateDTO = new UserUpdateDTO();
        updateDTO.setEmail("existing@example.com");

        when(userService.updateUser(eq(1L), any(UserUpdateDTO.class)))
                .thenThrow(new DuplicateResourceException("User with email existing@example.com already exists"));

        mockMvc.perform(put("/api/v1/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.error", is("Conflict")));
    }

    @Test
    @DisplayName("Resource not found on update: 404 при попытке обновить несуществующего пользователя")
    void handleResourceNotFoundException_OnUpdate_Returns404() throws Exception {
        UserUpdateDTO updateDTO = new UserUpdateDTO();
                updateDTO.setName("New Name");

        when(userService.updateUser(eq(999L), any(UserUpdateDTO.class)))
                .thenThrow(new ResourceNotFoundException("User not found with id: 999"));

        mockMvc.perform(put("/api/v1/users/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)));
    }

    @Test
    @DisplayName("Resource not found on delete: 404 при попытке удалить несуществующего пользователя")
    void handleResourceNotFoundException_OnDelete_Returns404() throws Exception {
        doThrow(new ResourceNotFoundException("User not found with id: 999"))
                .when(userService).deleteUser(999L);

        mockMvc.perform(delete("/api/v1/users/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)));
    }
}
