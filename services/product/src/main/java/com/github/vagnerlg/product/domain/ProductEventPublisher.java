package com.github.vagnerlg.product.domain;

import com.github.vagnerlg.product.domain.event.ProductCreatedEvent;
import com.github.vagnerlg.product.domain.event.ProductDeletedEvent;
import com.github.vagnerlg.product.domain.event.ProductUpdatedEvent;

public interface ProductEventPublisher {
    void publish(ProductCreatedEvent event);
    void publish(ProductUpdatedEvent event);
    void publish(ProductDeletedEvent event);
}
