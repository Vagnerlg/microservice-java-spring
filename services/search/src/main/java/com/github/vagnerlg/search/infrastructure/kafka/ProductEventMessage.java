package com.github.vagnerlg.search.infrastructure.kafka;

import java.math.BigDecimal;

record ProductEventMessage(String event, ProductData data) {

    record ProductData(
            String id,
            String name,
            String description,
            BigDecimal price,
            String category,
            String createdAt,
            String updatedAt
    ) {}
}
