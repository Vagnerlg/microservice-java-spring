package com.github.vagnerlg.search.domain;

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
) {}
