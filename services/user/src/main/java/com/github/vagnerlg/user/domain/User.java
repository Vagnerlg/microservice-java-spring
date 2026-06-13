package com.github.vagnerlg.user.domain;

import java.time.Instant;
import java.util.UUID;

public record User(UUID id, String keycloakId, String username, String name, Instant createdAt) {
}
