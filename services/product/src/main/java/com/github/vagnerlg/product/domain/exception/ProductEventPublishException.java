package com.github.vagnerlg.product.domain.exception;

public class ProductEventPublishException extends RuntimeException {

    public ProductEventPublishException(String productId, Throwable cause) {
        super("Failed to publish event for product: " + productId, cause);
    }
}
