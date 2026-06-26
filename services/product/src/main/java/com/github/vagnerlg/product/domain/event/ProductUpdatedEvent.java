package com.github.vagnerlg.product.domain.event;

import com.github.vagnerlg.product.domain.Product;

public record ProductUpdatedEvent(Product product) {
}
