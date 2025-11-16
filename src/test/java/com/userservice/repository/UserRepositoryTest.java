package com.userservice.repository;

import com.userservice.base.BaseRepositoryTest;
import com.userservice.entity.UserEntity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

@DataJpaTest
@DisplayName("UserRepository Tests")
class UserRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager entityManager;

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("existsByEmail: возврат true для существующего email")
    void existsByEmail_ReturnsTrue() {
        UserEntity user = createUser("Test", "test@example.com", 25);
        entityManager.persistAndFlush(user);

        assertThat(userRepository.existsByEmail("test@example.com")).isTrue();
    }

    @Test
    @DisplayName("existsByEmail: case-sensitive проверка")
    void existsByEmail_IsCaseSensitive() {
        UserEntity user = createUser("Test", "test@example.com", 25);
        entityManager.persistAndFlush(user);

        assertThat(userRepository.existsByEmail("TEST@EXAMPLE.COM")).isFalse();
    }

    @Test
    @DisplayName("Unique ограничение на email работает")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void uniqueEmailConstraint_ThrowsException() {
        userRepository.save(createUser("User1", "duplicate@example.com", 25));

        assertThatThrownBy(() ->
                userRepository.save(createUser("User2", "duplicate@example.com", 30))
        ).isInstanceOf(DataIntegrityViolationException.class);

        userRepository.deleteAll();
    }

    @Test
    @DisplayName("@PrePersist устанавливает createdAt автоматически")
    void prePersist_SetsCreatedAt() {
        UserEntity user = createUser("Test", "test@example.com", 25);
        user.setCreatedAt(null);

        UserEntity saved = userRepository.save(user);
        entityManager.flush();

        assertThat(saved.getCreatedAt()).isNotNull();
    }

    private UserEntity createUser(String name, String email, Integer age) {
        UserEntity user = new UserEntity();
        user.setName(name);
        user.setEmail(email);
        user.setAge(age);
        return user;
    }
}