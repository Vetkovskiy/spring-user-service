package com.userservice.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO для ответа API с данными пользователя
 */
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
public class UserResponseDTO {

    /**
     * Уникальный идентификатор пользователя.
     */
    private Long id;

    /**
     * Имя пользователя.
     */
    private String name;

    /**
     * Электронная почта пользователя.
     */
    private String email;

    /**
     * Возраст пользователя.
     */
    private Integer age;

    /**
     * Дата и время создания пользователя.
     * <p>Форматируется в JSON как {@code yyyy-MM-dd'T'HH:mm:ss}.</p>
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
}
