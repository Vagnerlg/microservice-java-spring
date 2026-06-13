package com.github.vagnerlg.auth.application;

import com.github.vagnerlg.auth.domain.User;
import com.github.vagnerlg.auth.infrastructure.kafka.KafkaUserEventPublisher;
import com.github.vagnerlg.auth.infrastructure.keycloak.KeycloakClient;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class RegisterUserService {

    private final KeycloakClient keycloakClient;
    private final KafkaUserEventPublisher eventPublisher;

    public RegisterUserService(KeycloakClient keycloakClient, KafkaUserEventPublisher eventPublisher) {
        this.keycloakClient = keycloakClient;
        this.eventPublisher = eventPublisher;
    }

    public User register(String username, String name, String password) {
        String keycloakId = keycloakClient.createUser(username, name, password);
        var user = new User(keycloakId, username, name, Instant.now());
        eventPublisher.publish(user);
        return user;
    }
}
