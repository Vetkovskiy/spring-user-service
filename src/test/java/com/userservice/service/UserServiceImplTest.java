package com.userservice.service;

import com.userservice.dto.UserCreateDTO;
import com.userservice.dto.UserResponseDTO;
import com.userservice.dto.UserUpdateDTO;
import com.userservice.entity.UserEntity;
import com.userservice.exception.DuplicateResourceException;
import com.userservice.exception.ResourceNotFoundException;
import com.userservice.mapper.UserMapper;
import com.userservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserServiceImpl Unit Tests")
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserServiceImpl userService;

    private UserCreateDTO createDTO;
    private UserUpdateDTO updateDTO;
    private UserEntity userEntity;
    private UserResponseDTO responseDTO;

    @BeforeEach
    void setUp() {
        createDTO = UserCreateDTO.builder()
                .name("John Doe")
                .email("john@example.com")
                .age(30)
                .build();

        updateDTO = new UserUpdateDTO("Jane Doe", "jane@example.com", 25);

        userEntity = new UserEntity(1L, "John Doe", "john@example.com", 30, LocalDateTime.now());


        responseDTO = UserResponseDTO.builder()
                .id(1L)
                .name("John Doe")
                .email("john@example.com")
                .age(30)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("createUser: успешное создание пользователя")
    void createUser_Success() {
        when(userRepository.existsByEmail(createDTO.getEmail())).thenReturn(false);
        when(userMapper.ofDTO(createDTO)).thenReturn(userEntity);
        when(userRepository.save(any(UserEntity.class))).thenReturn(userEntity);
        when(userMapper.ofEntity(userEntity)).thenReturn(responseDTO);

        UserResponseDTO result = userService.createUser(createDTO);

        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo(createDTO.getEmail());
        assertThat(result.getName()).isEqualTo(createDTO.getName());

        verify(userRepository).existsByEmail(createDTO.getEmail());
        verify(userMapper).ofDTO(createDTO);
        verify(userRepository).save(any(UserEntity.class));
        verify(userMapper).ofEntity(userEntity);
    }

    @Test
    @DisplayName("createUser: выброс DuplicateResourceException если email существует")
    void createUser_ThrowsExceptionWhenEmailExists() {
        when(userRepository.existsByEmail(createDTO.getEmail())).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(createDTO))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already exists");

        verify(userRepository).existsByEmail(createDTO.getEmail());
        verify(userMapper, never()).ofDTO(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("getUserById: успешное получение пользователя")
    void getUserById_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(userEntity));
        when(userMapper.ofEntity(userEntity)).thenReturn(responseDTO);

        UserResponseDTO result = userService.getUserById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);

        verify(userRepository).findById(1L);
        verify(userMapper).ofEntity(userEntity);
    }

    @Test
    @DisplayName("getUserById: выброс ResourceNotFoundException если пользователь не найден")
    void getUserById_ThrowsExceptionWhenNotFound() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("not found");

        verify(userRepository).findById(999L);
        verify(userMapper, never()).ofEntity(any());
    }

    @Test
    @DisplayName("getAllUsers: успешное получение всех пользователей")
    void getAllUsers_Success() {
        UserEntity user2 = new UserEntity(
                2L, "Jane Smith", "jane@example.com", 25, LocalDateTime.now());

        UserResponseDTO response2 = UserResponseDTO.builder()
                .id(2L)
                .name("Jane Smith")
                .email("jane@example.com")
                .age(25)
                .createdAt(LocalDateTime.now())
                .build();

        when(userRepository.findAll()).thenReturn(List.of(userEntity, user2));
        when(userMapper.ofEntity(userEntity)).thenReturn(responseDTO);
        when(userMapper.ofEntity(user2)).thenReturn(response2);

        List<UserResponseDTO> result = userService.getAllUsers();

        assertThat(result).hasSize(2);
        assertThat(result).extracting(UserResponseDTO::getId).containsExactly(1L, 2L);

        verify(userRepository).findAll();
        verify(userMapper, times(2)).ofEntity(any(UserEntity.class));
    }

    @Test
    @DisplayName("getAllUsers: возврат пустого списка когда пользователей нет")
    void getAllUsers_ReturnsEmptyListWhenNoUsers() {
        when(userRepository.findAll()).thenReturn(List.of());

        List<UserResponseDTO> result = userService.getAllUsers();

        assertThat(result).isEmpty();
        verify(userRepository).findAll();
    }

    @Test
    @DisplayName("updateUser: успешное обновление пользователя")
    void updateUser_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(userEntity));
        when(userRepository.existsByEmail(updateDTO.getEmail())).thenReturn(false);
        when(userRepository.save(any(UserEntity.class))).thenReturn(userEntity);
        when(userMapper.ofEntity(userEntity)).thenReturn(responseDTO);

        UserResponseDTO result = userService.updateUser(1L, updateDTO);

        assertThat(result).isNotNull();

        verify(userRepository).findById(1L);
        verify(userMapper).updateEntityFromDTO(eq(updateDTO), eq(userEntity));
        verify(userRepository).save(userEntity);
        verify(userMapper).ofEntity(userEntity);
    }

    @Test
    @DisplayName("updateUser: успешное обновление когда email не изменился")
    void updateUser_SuccessWhenEmailNotChanged() {
        updateDTO.setEmail("john@example.com"); // тот же email
        when(userRepository.findById(1L)).thenReturn(Optional.of(userEntity));
        when(userRepository.save(any(UserEntity.class))).thenReturn(userEntity);
        when(userMapper.ofEntity(userEntity)).thenReturn(responseDTO);

        UserResponseDTO result = userService.updateUser(1L, updateDTO);

        assertThat(result).isNotNull();

        verify(userRepository).findById(1L);
        verify(userRepository, never()).existsByEmail(anyString());
        verify(userRepository).save(userEntity);
    }

    @Test
    @DisplayName("updateUser: успешное обновление когда email null")
    void updateUser_SuccessWhenEmailIsNull() {
        updateDTO.setEmail(null);
        when(userRepository.findById(1L)).thenReturn(Optional.of(userEntity));
        when(userRepository.save(any(UserEntity.class))).thenReturn(userEntity);
        when(userMapper.ofEntity(userEntity)).thenReturn(responseDTO);

        UserResponseDTO result = userService.updateUser(1L, updateDTO);

        assertThat(result).isNotNull();

        verify(userRepository).findById(1L);
        verify(userRepository, never()).existsByEmail(anyString());
    }

    @Test
    @DisplayName("updateUser: выброс ResourceNotFoundException если пользователь не найден")
    void updateUser_ThrowsExceptionWhenUserNotFound() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateUser(999L, updateDTO))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("not found");

        verify(userRepository).findById(999L);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateUser: выброс DuplicateResourceException если новый email уже существует")
    void updateUser_ThrowsExceptionWhenNewEmailExists() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(userEntity));
        when(userRepository.existsByEmail(updateDTO.getEmail())).thenReturn(true);

        assertThatThrownBy(() -> userService.updateUser(1L, updateDTO))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already exists");

        verify(userRepository).findById(1L);
        verify(userRepository).existsByEmail(updateDTO.getEmail());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("deleteUser: успешное удаление пользователя")
    void deleteUser_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(userEntity));
        doNothing().when(userRepository).delete(userEntity);
        userService.deleteUser(1L);

        verify(userRepository).findById(1L);
        verify(userRepository).delete(userEntity);
    }

    @Test
    @DisplayName("deleteUser: выброс ResourceNotFoundException если пользователь не найден")
    void deleteUser_ThrowsExceptionWhenNotFound() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.deleteUser(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("not found");

        verify(userRepository).findById(999L);
        verify(userRepository, never()).delete(any());
    }

    @Test
    @DisplayName("existsByEmail: возврат true если email существует")
    void existsByEmail_ReturnsTrue() {
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        boolean result = userService.existsByEmail("test@example.com");

        assertThat(result).isTrue();
        verify(userRepository).existsByEmail("test@example.com");
    }

    @Test
    @DisplayName("existsByEmail: возврат false если email не существует")
    void existsByEmail_ReturnsFalse() {
        when(userRepository.existsByEmail("nonexistent@example.com")).thenReturn(false);

        boolean result = userService.existsByEmail("nonexistent@example.com");

        assertThat(result).isFalse();
        verify(userRepository).existsByEmail("nonexistent@example.com");
    }
}
