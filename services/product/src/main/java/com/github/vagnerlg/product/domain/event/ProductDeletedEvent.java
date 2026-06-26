package com.github.vagnerlg.product.domain.event;

import com.github.vagnerlg.product.domain.Product;

public record ProductDeletedEvent(Product product) {
}
