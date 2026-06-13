package com.github.vagnerlg.auth.domain;

import java.time.Instant;

public record User(String keycloakId, String username, String name, Instant createdAt) {
}
