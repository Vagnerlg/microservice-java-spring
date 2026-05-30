package com.github.vagnerlg.search.infrastructure.web;

import com.github.vagnerlg.search.domain.Product;

import java.math.BigDecimal;
import java.time.Instant;

public record ProductSearchResponse(
        String id,
        String name,
        String description,
        BigDecimal price,
        String category,
        Instant createdAt,
        Instant updatedAt
) {
    static ProductSearchResponse from(Product product) {
        return new ProductSearchResponse(
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
