package com.github.vagnerlg.user.domain.exception;

public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(String keycloakId) {
        super("User not found: " + keycloakId);
    }
}
