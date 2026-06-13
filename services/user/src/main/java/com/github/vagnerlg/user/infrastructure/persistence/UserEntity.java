package com.github.vagnerlg.user.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
class UserEntity {

    @Id
    UUID id;

    @Column(name = "keycloak_id", nullable = false, unique = true)
    String keycloakId;

    @Column(nullable = false)
    String username;

    @Column(nullable = false)
    String name;

    @Column(name = "created_at", nullable = false)
    Instant createdAt;

    UserEntity() {
    }

    UserEntity(UUID id, String keycloakId, String username, String name, Instant createdAt) {
        this.id = id;
        this.keycloakId = keycloakId;
        this.username = username;
        this.name = name;
        this.createdAt = createdAt;
    }

    UUID id() { return id; }
    String keycloakId() { return keycloakId; }
    String username() { return username; }
    String name() { return name; }
    Instant createdAt() { return createdAt; }
}
