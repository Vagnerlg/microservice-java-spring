package com.github.vagnerlg.order.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record Order(
        UUID id,
        String userId,
        List<OrderItem> items,
        OrderStatus status,
        BigDecimal totalPrice,
        String cancellationReason,
        Instant createdAt,
        Instant updatedAt
) {
}
