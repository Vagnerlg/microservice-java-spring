package com.github.vagnerlg.inventory.infrastructure.kafka;

import com.github.vagnerlg.inventory.domain.StockReservationPublisher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;

@Component
class KafkaStockReservationPublisher implements StockReservationPublisher {

    private final KafkaTemplate<Object, Object> kafkaTemplate;
    private final String topic;

    KafkaStockReservationPublisher(KafkaTemplate<Object, Object> kafkaTemplate,
                                   @Value("${kafka.topics.stock-reservation}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    @Override
    public void publishReserved(String orderId) {
        send("RESERVED", orderId, null);
    }

    @Override
    public void publishUnavailable(String orderId, String reason) {
        send("UNAVAILABLE", orderId, reason);
    }

    private void send(String event, String orderId, String reason) {
        var message = new StockReservationEventMessage(event,
                new StockReservationEventMessage.ReservationData(orderId, reason));
        try {
            kafkaTemplate.send(topic, orderId, message).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while publishing stock reservation event", e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("Failed to publish stock reservation event", e.getCause());
        }
    }
}
