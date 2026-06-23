package com.github.vagnerlg.order.infrastructure.web.response;

import com.github.vagnerlg.order.domain.OrderItem;

import java.math.BigDecimal;

public record OrderItemResponse(String productId, String name, BigDecimal price, int quantity, BigDecimal subtotal) {

    public static OrderItemResponse from(OrderItem item) {
        return new OrderItemResponse(
                item.productId(),
                item.name(),
                item.price(),
                item.quantity(),
                item.price().multiply(BigDecimal.valueOf(item.quantity()))
        );
    }
}
