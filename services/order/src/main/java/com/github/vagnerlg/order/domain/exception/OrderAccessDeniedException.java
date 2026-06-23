package com.github.vagnerlg.order.domain.exception;

public class OrderAccessDeniedException extends RuntimeException {
    public OrderAccessDeniedException() {
        super("Access denied to this order");
    }
}
