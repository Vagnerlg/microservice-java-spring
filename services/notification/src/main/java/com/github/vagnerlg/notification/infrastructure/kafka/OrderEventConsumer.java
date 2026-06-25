package com.github.vagnerlg.notification.infrastructure.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class OrderEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderEventConsumer.class);

    private final ObjectMapper mapper;

    public OrderEventConsumer(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @KafkaListener(topics = "${kafka.topics.order}")
    public void consume(ConsumerRecord<String, String> record) {
        try {
            var message = mapper.readValue(record.value(), OrderEventMessage.class);
            switch (message.event()) {
                case "CREATED" -> log.info("[notification] order.CREATED orderId={} userId={} totalPrice={}",
                        message.data().orderId(), message.data().userId(), message.data().totalPrice());
                case "CANCELLED" -> log.info("[notification] order.CANCELLED orderId={} userId={}",
                        message.data().orderId(), message.data().userId());
                default -> log.warn("[notification] order event ignored type={}", message.event());
            }
        } catch (Exception e) {
            log.error("[notification] failed to process order event key={}: {}", record.key(), e.getMessage());
        }
    }
}
