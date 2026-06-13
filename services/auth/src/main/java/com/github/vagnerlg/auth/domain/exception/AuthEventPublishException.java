package com.github.vagnerlg.auth.domain.exception;

public class AuthEventPublishException extends RuntimeException {

    public AuthEventPublishException(String username, Throwable cause) {
        super("Failed to publish auth event for user: " + username, cause);
    }
}
