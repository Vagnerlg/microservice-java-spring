package com.github.vagnerlg.search.infrastructure.kafka;

import java.math.BigDecimal;
import java.time.Instant;

record ProductEventMessage(String event, ProductData data) {

    record ProductData(
            String id,
            String name,
            String description,
            BigDecimal price,
            String category,
            Instant createdAt,
            Instant updatedAt
    ) {}
}
