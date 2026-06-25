package com.github.vagnerlg.notification.infrastructure.kafka;

import java.math.BigDecimal;
import java.time.Instant;

record OrderEventMessage(String event, OrderData data) {

    record OrderData(String orderId, String userId, BigDecimal totalPrice, Instant createdAt) {
    }
}
