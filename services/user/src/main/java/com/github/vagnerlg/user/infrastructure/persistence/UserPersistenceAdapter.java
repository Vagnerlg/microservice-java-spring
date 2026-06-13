package com.github.vagnerlg.user.infrastructure.persistence;

import com.github.vagnerlg.user.domain.User;
import com.github.vagnerlg.user.domain.UserRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
class UserPersistenceAdapter implements UserRepository {

    private final UserJpaRepository jpaRepository;

    UserPersistenceAdapter(UserJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public User save(User user) {
        var saved = jpaRepository.save(toEntity(user));
        return toDomain(saved);
    }

    @Override
    public boolean existsByKeycloakId(String keycloakId) {
        return jpaRepository.existsByKeycloakId(keycloakId);
    }

    @Override
    public Optional<User> findByKeycloakId(String keycloakId) {
        return jpaRepository.findByKeycloakId(keycloakId).map(this::toDomain);
    }

    private UserEntity toEntity(User user) {
        return new UserEntity(user.id(), user.keycloakId(), user.username(), user.name(), user.createdAt());
    }

    private User toDomain(UserEntity entity) {
        return new User(entity.id(), entity.keycloakId(), entity.username(), entity.name(), entity.createdAt());
    }
}
