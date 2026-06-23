package com.github.vagnerlg.cart.domain.exception;

public class CartEmptyException extends RuntimeException {

    public CartEmptyException(String userId) {
        super("Cart is empty for user: " + userId);
    }
}
