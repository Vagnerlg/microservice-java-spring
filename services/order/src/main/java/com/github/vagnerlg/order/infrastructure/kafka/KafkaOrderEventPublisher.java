package com.github.vagnerlg.order.infrastructure.kafka;

import com.github.vagnerlg.order.domain.Order;
import com.github.vagnerlg.order.domain.OrderEventPublisher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;

@Component
class KafkaOrderEventPublisher implements OrderEventPublisher {

    private final KafkaTemplate<Object, Object> kafkaTemplate;
    private final String topic;

    KafkaOrderEventPublisher(KafkaTemplate<Object, Object> kafkaTemplate,
                             @Value("${kafka.topics.order}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    @Override
    public void publishCreated(Order order) {
        send("CREATED", order);
    }

    @Override
    public void publishCancelled(Order order) {
        send("CANCELLED", order);
    }

    private void send(String event, Order order) {
        var data = new OrderEventMessage.OrderData(
                order.id().toString(), order.userId(), order.totalPrice(), order.createdAt());
        var message = new OrderEventMessage(event, data);
        try {
            kafkaTemplate.send(topic, order.id().toString(), message).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while publishing order event", e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("Failed to publish order event", e.getCause());
        }
    }
}
