package com.userservice.service;

import com.userservice.dto.UserCreateDTO;
import com.userservice.dto.UserResponseDTO;
import com.userservice.dto.UserUpdateDTO;
import com.userservice.exception.DuplicateResourceException;
import com.userservice.exception.ResourceNotFoundException;

import java.util.List;

/**
 * Сервисный интерфейс для управления пользователями.
 *
 * <p>Определяет контракт бизнес-логики, связанной с CRUD-операциями
 * над пользователями.
 * Основные операции:
 * <ul>
 *   <li>Создание нового пользователя</li>
 *   <li>Получение пользователя по идентификатору</li>
 *   <li>Получение списка всех пользователей</li>
 *   <li>Обновление данных пользователя</li>
 *   <li>Удаление пользователя</li>
 *   <li>Проверка существования пользователя по email</li>
 * </ul>
 */
public interface UserService {

    /**
     * Создаёт нового пользователя на основе переданных данных.
     *
     * @param request DTO с данными для создания пользователя
     * @return DTO с информацией о созданном пользователе
     * @throws DuplicateResourceException если пользователь с таким email уже существует
     */
    UserResponseDTO createUser(UserCreateDTO request);

    /**
     * Возвращает пользователя по его идентификатору.
     *
     * @param id уникальный идентификатор пользователя
     * @return DTO с информацией о найденном пользователе
     * @throws ResourceNotFoundException если пользователь не найден
     */
    UserResponseDTO getUserById(Long id);

    /**
     * Возвращает список всех пользователей.
     *
     * @return список DTO с информацией о пользователях
     */
    List<UserResponseDTO> getAllUsers();

    /**
     * Обновляет данные существующего пользователя.
     *
     * @param id      идентификатор пользователя для обновления
     * @param request DTO с новыми данными
     * @return DTO с обновлённой информацией о пользователе
     * @throws ResourceNotFoundException  если пользователь не найден
     * @throws DuplicateResourceException если новый email уже занят другим пользователем
     */
    UserResponseDTO updateUser(Long id, UserUpdateDTO request);

    /**
     * Удаляет пользователя по идентификатору.
     *
     * @param id идентификатор пользователя
     * @throws ResourceNotFoundException если пользователь не найден
     */
    void deleteUser(Long id);

    /**
     * Проверяет существование пользователя по email.
     *
     * @param email адрес электронной почты
     * @return true, если пользователь с таким email существует,
     * иначе false
     */
    boolean existsByEmail(String email);
}