package com.github.vagnerlg.user.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface UserJpaRepository extends JpaRepository<UserEntity, UUID> {

    boolean existsByKeycloakId(String keycloakId);

    Optional<UserEntity> findByKeycloakId(String keycloakId);
}
