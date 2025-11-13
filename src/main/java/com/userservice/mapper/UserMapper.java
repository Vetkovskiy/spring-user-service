package com.userservice.mapper;

import com.userservice.dto.UserCreateDTO;
import com.userservice.dto.UserResponseDTO;
import com.userservice.dto.UserUpdateDTO;
import com.userservice.entity.UserEntity;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

/**
 * Mapper для преобразования между Entity и DTO
 */
@Mapper(componentModel = "spring")
public interface UserMapper {

    /**
     * Преобразование Entity → ResponseDTO
     */
    UserResponseDTO ofEntity(UserEntity user);

    /**
     * Преобразование UserCreateDto → Entity (для создания)
     */
    UserEntity ofDTO(UserCreateDTO dto);

    /**
     * Обновление существующего Entity из UserUpdateDto
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromDTO(UserUpdateDTO dto, @MappingTarget UserEntity user);
}
