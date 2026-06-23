package com.github.vagnerlg.cart.infrastructure.kafka;

import com.github.vagnerlg.cart.domain.Cart;
import com.github.vagnerlg.cart.domain.CartEventPublisher;
import com.github.vagnerlg.cart.domain.exception.CartEventPublishException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.ExecutionException;

@Component
class KafkaCartEventPublisher implements CartEventPublisher {

    private final KafkaTemplate<Object, Object> kafkaTemplate;
    private final String topic;

    KafkaCartEventPublisher(KafkaTemplate<Object, Object> kafkaTemplate,
                            @Value("${kafka.topics.cart}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    @Override
    public void publishCheckout(Cart cart) {
        var items = cart.items().stream()
                .map(i -> new CartEventMessage.CartItemData(i.productId(), i.name(), i.price(), i.quantity()))
                .toList();
        var data = new CartEventMessage.CartData(cart.userId(), items, Instant.now());
        var message = new CartEventMessage("CHECKOUT", data);
        try {
            kafkaTemplate.send(topic, cart.userId(), message).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CartEventPublishException(cart.userId(), e);
        } catch (ExecutionException e) {
            throw new CartEventPublishException(cart.userId(), e.getCause());
        }
    }
}
