package com.github.vagnerlg.cart.domain.exception;

public class CartEventPublishException extends RuntimeException {

    public CartEventPublishException(String userId, Throwable cause) {
        super("Failed to publish cart event for user: " + userId, cause);
    }
}
