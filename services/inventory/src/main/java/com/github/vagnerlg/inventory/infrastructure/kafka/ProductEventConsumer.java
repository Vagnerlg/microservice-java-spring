package com.github.vagnerlg.inventory.infrastructure.kafka;

import com.github.vagnerlg.inventory.application.InventoryService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class ProductEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(ProductEventConsumer.class);

    private final InventoryService inventoryService;
    private final ObjectMapper mapper;

    public ProductEventConsumer(InventoryService inventoryService, ObjectMapper mapper) {
        this.inventoryService = inventoryService;
        this.mapper = mapper;
    }

    @KafkaListener(topics = "${kafka.topics.product}")
    public void consume(ConsumerRecord<String, String> record) {
        try {
            var message = mapper.readValue(record.value(), ProductEventMessage.class);
            if ("CREATED".equals(message.event())) {
                inventoryService.initializeStock(message.data().id());
            } else {
                log.warn("Ignoring unknown product event type: {}", message.event());
            }
        } catch (Exception e) {
            log.error("Failed to process product event key={}: {}", record.key(), e.getMessage());
        }
    }
}
