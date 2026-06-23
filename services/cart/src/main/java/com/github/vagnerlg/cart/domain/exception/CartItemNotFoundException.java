package com.github.vagnerlg.cart.domain.exception;

public class CartItemNotFoundException extends RuntimeException {

    public CartItemNotFoundException(String productId) {
        super("Item not found in cart: " + productId);
    }
}
