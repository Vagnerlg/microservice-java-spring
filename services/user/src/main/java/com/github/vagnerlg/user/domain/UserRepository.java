package com.github.vagnerlg.user.domain;

import java.util.Optional;

public interface UserRepository {

    User save(User user);

    boolean existsByKeycloakId(String keycloakId);

    Optional<User> findByKeycloakId(String keycloakId);
}
