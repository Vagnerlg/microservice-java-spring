package com.github.vagnerlg.product.infrastructure.web.response;

import com.github.vagnerlg.product.domain.Product;

import java.math.BigDecimal;
import java.time.Instant;

public record ProductResponse(
        String id,
        String name,
        String description,
        BigDecimal price,
        String category,
        Instant createdAt,
        Instant updatedAt
) {
    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.id(),
                product.name(),
                product.description(),
                product.price(),
                product.category(),
                product.createdAt(),
                product.updatedAt()
        );
    }
}
