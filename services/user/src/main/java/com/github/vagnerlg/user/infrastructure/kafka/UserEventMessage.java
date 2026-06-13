package com.github.vagnerlg.user.infrastructure.kafka;

import java.time.Instant;

record UserEventMessage(String event, UserData data) {

    record UserData(String keycloakId, String username, String name, Instant createdAt) {
    }
}
