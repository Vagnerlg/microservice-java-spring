package com.github.vagnerlg.product.domain;

import com.github.vagnerlg.product.domain.event.ProductCreatedEvent;

public interface ProductEventPublisher {
    void publish(ProductCreatedEvent event);
}
