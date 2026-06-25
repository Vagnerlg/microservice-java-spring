package com.github.vagnerlg.notification.infrastructure.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class StockLevelEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(StockLevelEventConsumer.class);

    private final ObjectMapper mapper;

    public StockLevelEventConsumer(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @KafkaListener(topics = "${kafka.topics.stock-level}")
    public void consume(ConsumerRecord<String, String> record) {
        try {
            var message = mapper.readValue(record.value(), StockLevelEventMessage.class);
            if ("LOW".equals(message.event())) {
                log.warn("[notification] stock-level.LOW productId={} currentQuantity={}",
                        message.data().productId(), message.data().currentQuantity());
            } else {
                log.warn("[notification] stock-level event ignored type={}", message.event());
            }
        } catch (Exception e) {
            log.error("[notification] failed to process stock-level event key={}: {}", record.key(), e.getMessage());
        }
    }
}
