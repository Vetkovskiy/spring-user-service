package com.userservice.repository;

import com.userservice.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA Repository для UserEntity
 */
@Repository
 public interface UserRepository extends JpaRepository<UserEntity,Long> {

    /**
     * Проверить существование по email
     */
    boolean existsByEmail(String email);

}
