package com.github.vagnerlg.cart.infrastructure.kafka;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

record CartEventMessage(String event, CartData data) {

    record CartData(String userId, List<CartItemData> items, Instant checkoutAt) {}

    record CartItemData(String productId, String name, BigDecimal price, int quantity) {}
}
