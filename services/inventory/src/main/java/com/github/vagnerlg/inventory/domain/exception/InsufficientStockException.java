package com.github.vagnerlg.inventory.domain.exception;

public class InsufficientStockException extends RuntimeException {
    public InsufficientStockException(String productId, int requested, int available) {
        super("Insufficient stock for product " + productId + ": requested=" + requested + ", available=" + available);
    }
}
