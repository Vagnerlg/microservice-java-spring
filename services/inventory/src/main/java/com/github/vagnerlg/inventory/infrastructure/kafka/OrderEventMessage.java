package com.github.vagnerlg.inventory.infrastructure.kafka;

import java.util.List;

record OrderEventMessage(String event, OrderData data) {

    record OrderData(String orderId, String userId, List<OrderItemData> items) {
    }

    record OrderItemData(String productId, int quantity) {
    }
}
