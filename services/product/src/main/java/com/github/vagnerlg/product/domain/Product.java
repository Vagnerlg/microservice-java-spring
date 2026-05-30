package com.github.vagnerlg.product.domain;

import java.math.BigDecimal;
import java.time.Instant;

public record Product(
        String id,
        String name,
        String description,
        BigDecimal price,
        String category,
        Instant createdAt,
        Instant updatedAt
) {
}
