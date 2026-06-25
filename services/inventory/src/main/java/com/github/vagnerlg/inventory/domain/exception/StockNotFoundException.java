package com.github.vagnerlg.inventory.domain.exception;

public class StockNotFoundException extends RuntimeException {
    public StockNotFoundException(String productId) {
        super("Stock not found for product: " + productId);
    }
}
