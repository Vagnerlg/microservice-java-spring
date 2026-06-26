package com.github.vagnerlg.product.application;

import java.math.BigDecimal;

public record UpdateProduct(
        String id,
        String name,
        String description,
        BigDecimal price,
        String category
) {
}
