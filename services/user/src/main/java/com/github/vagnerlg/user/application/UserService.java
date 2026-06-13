package com.github.vagnerlg.user.application;

import com.github.vagnerlg.user.domain.User;
import com.github.vagnerlg.user.domain.UserRepository;
import com.github.vagnerlg.user.domain.exception.UserNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void create(String keycloakId, String username, String name, Instant createdAt) {
        if (userRepository.existsByKeycloakId(keycloakId)) {
            log.warn("Ignoring duplicate user event: keycloakId={}", keycloakId);
            return;
        }
        userRepository.save(new User(UUID.randomUUID(), keycloakId, username, name, createdAt));
    }

    public User findByKeycloakId(String keycloakId) {
        return userRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new UserNotFoundException(keycloakId));
    }
}
