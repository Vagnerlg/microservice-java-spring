package com.github.vagnerlg.auth.infrastructure.kafka;

import com.github.vagnerlg.auth.domain.User;

import java.time.Instant;

record UserEventMessage(String event, UserData data) {

    record UserData(String keycloakId, String username, String name, Instant createdAt) {
    }

    static UserEventMessage created(User user) {
        return new UserEventMessage("CREATED",
                new UserData(user.keycloakId(), user.username(), user.name(), user.createdAt()));
    }
}
