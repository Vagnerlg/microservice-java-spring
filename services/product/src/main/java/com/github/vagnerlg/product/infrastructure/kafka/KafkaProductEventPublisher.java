package com.github.vagnerlg.product.infrastructure.kafka;

import com.github.vagnerlg.product.domain.Product;
import com.github.vagnerlg.product.domain.ProductEventPublisher;
import com.github.vagnerlg.product.domain.event.ProductCreatedEvent;
import com.github.vagnerlg.product.domain.event.ProductDeletedEvent;
import com.github.vagnerlg.product.domain.event.ProductUpdatedEvent;
import com.github.vagnerlg.product.domain.exception.ProductEventPublishException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;

@Component
class KafkaProductEventPublisher implements ProductEventPublisher {

    private final KafkaTemplate<Object, Object> kafkaTemplate;
    private final String topic;

    KafkaProductEventPublisher(KafkaTemplate<Object, Object> kafkaTemplate,
                               @Value("${kafka.topics.product}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    @Override
    public void publish(ProductCreatedEvent event) {
        send("CREATED", event.product());
    }

    @Override
    public void publish(ProductUpdatedEvent event) {
        send("UPDATED", event.product());
    }

    @Override
    public void publish(ProductDeletedEvent event) {
        send("DELETED", event.product());
    }

    private void send(String eventType, Product product) {
        var message = new ProductEventMessage(eventType, product);
        try {
            kafkaTemplate.send(topic, product.id(), message).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ProductEventPublishException(product.id(), e);
        } catch (ExecutionException e) {
            throw new ProductEventPublishException(product.id(), e.getCause());
        }
    }
}
