package com.github.vagnerlg.product.infrastructure.kafka;

import com.github.vagnerlg.product.domain.ProductEventPublisher;
import com.github.vagnerlg.product.domain.event.ProductCreatedEvent;
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
        var message = new ProductEventMessage("CREATED", event.product());
        try {
            kafkaTemplate.send(topic, event.product().id(), message).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ProductEventPublishException(event.product().id(), e);
        } catch (ExecutionException e) {
            throw new ProductEventPublishException(event.product().id(), e.getCause());
        }
    }
}
