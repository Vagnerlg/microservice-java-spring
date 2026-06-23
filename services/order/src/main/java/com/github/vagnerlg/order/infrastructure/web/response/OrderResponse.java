package com.github.vagnerlg.order.infrastructure.web.response;

import com.github.vagnerlg.order.domain.Order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        String userId,
        List<OrderItemResponse> items,
        String status,
        BigDecimal totalPrice,
        String cancellationReason,
        Instant createdAt,
        Instant updatedAt
) {

    public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.id(),
                order.userId(),
                order.items().stream().map(OrderItemResponse::from).toList(),
                order.status().name(),
                order.totalPrice(),
                order.cancellationReason(),
                order.createdAt(),
                order.updatedAt()
        );
    }
}
