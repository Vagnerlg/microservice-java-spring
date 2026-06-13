package com.github.vagnerlg.user.infrastructure.web.response;

import com.github.vagnerlg.user.domain.User;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(UUID id, String username, String name, Instant createdAt) {

    public static UserResponse from(User user) {
        return new UserResponse(user.id(), user.username(), user.name(), user.createdAt());
    }
}
