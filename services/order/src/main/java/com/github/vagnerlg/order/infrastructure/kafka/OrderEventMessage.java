package com.github.vagnerlg.order.infrastructure.kafka;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

record OrderEventMessage(String event, OrderData data) {

    record OrderData(String orderId, String userId, BigDecimal totalPrice, Instant createdAt,
                     List<OrderItemData> items) {
    }

    record OrderItemData(String productId, int quantity) {
    }
}
