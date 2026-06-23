package com.github.vagnerlg.order.domain;

import java.math.BigDecimal;

public record CreateOrderItem(String productId, String name, BigDecimal price, int quantity) {
}
