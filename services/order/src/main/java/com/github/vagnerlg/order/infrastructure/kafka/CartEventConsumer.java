package com.github.vagnerlg.order.infrastructure.kafka;

import com.github.vagnerlg.order.application.OrderService;
import com.github.vagnerlg.order.domain.CreateOrderItem;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

@Component
public class CartEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(CartEventConsumer.class);

    private final OrderService orderService;
    private final ObjectMapper mapper;

    public CartEventConsumer(OrderService orderService, ObjectMapper mapper) {
        this.orderService = orderService;
        this.mapper = mapper;
    }

    @KafkaListener(topics = "${kafka.topics.cart}")
    public void consume(ConsumerRecord<String, String> record) {
        try {
            var message = mapper.readValue(record.value(), CartEventMessage.class);
            if ("CHECKOUT".equals(message.event())) {
                var data = message.data();
                List<CreateOrderItem> items = data.items().stream()
                        .map(i -> new CreateOrderItem(i.productId(), i.name(), i.price(), i.quantity()))
                        .toList();
                orderService.createFromCheckout(data.userId(), items);
            } else {
                log.warn("Ignoring unknown cart event type: {}", message.event());
            }
        } catch (Exception e) {
            log.error("Failed to process cart event key={}: {}", record.key(), e.getMessage());
        }
    }
}
