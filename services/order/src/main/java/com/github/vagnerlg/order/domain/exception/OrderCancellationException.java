package com.github.vagnerlg.order.domain.exception;

public class OrderCancellationException extends RuntimeException {
    public OrderCancellationException(String status) {
        super("Cannot cancel order with status: " + status);
    }
}
