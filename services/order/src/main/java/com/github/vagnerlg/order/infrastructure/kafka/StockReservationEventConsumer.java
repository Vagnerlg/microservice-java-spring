package com.github.vagnerlg.order.infrastructure.kafka;

import com.github.vagnerlg.order.application.OrderService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

@Component
public class StockReservationEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(StockReservationEventConsumer.class);

    private final OrderService orderService;
    private final ObjectMapper mapper;

    public StockReservationEventConsumer(OrderService orderService, ObjectMapper mapper) {
        this.orderService = orderService;
        this.mapper = mapper;
    }

    @KafkaListener(topics = "${kafka.topics.stock-reservation}")
    public void consume(ConsumerRecord<String, String> record) {
        try {
            var message = mapper.readValue(record.value(), StockReservationEventMessage.class);
            var orderId = UUID.fromString(message.data().orderId());
            switch (message.event()) {
                case "RESERVED" -> orderService.confirm(orderId);
                case "UNAVAILABLE" -> orderService.cancelBySystem(orderId, message.data().reason());
                default -> log.warn("Ignoring unknown stock-reservation event type: {}", message.event());
            }
        } catch (Exception e) {
            log.error("Failed to process stock-reservation event key={}: {}", record.key(), e.getMessage());
        }
    }
}
