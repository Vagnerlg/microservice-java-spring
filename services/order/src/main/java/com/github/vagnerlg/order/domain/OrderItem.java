package com.github.vagnerlg.order.domain;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItem(UUID id, String productId, String name, BigDecimal price, int quantity) {
}
