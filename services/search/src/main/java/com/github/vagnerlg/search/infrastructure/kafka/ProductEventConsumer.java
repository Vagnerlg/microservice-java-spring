package com.github.vagnerlg.search.infrastructure.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.vagnerlg.search.application.ProductSearchService;
import com.github.vagnerlg.search.domain.Product;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class ProductEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(ProductEventConsumer.class);

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final ProductSearchService service;

    public ProductEventConsumer(ProductSearchService service) {
        this.service = service;
    }

    @KafkaListener(topics = "${kafka.topics.product}")
    public void consume(ConsumerRecord<String, String> record) {
        try {
            var message = MAPPER.readValue(record.value(), ProductEventMessage.class);
            switch (message.event()) {
                case "CREATED", "UPDATED" -> service.index(toProduct(message.data()));
                case "DELETED" -> service.delete(message.data().id());
                default -> log.warn("Ignoring unknown product event type: {}", message.event());
            }
        } catch (Exception e) {
            log.error("Failed to process product event key={}: {}", record.key(), e.getMessage());
        }
    }

    private Product toProduct(ProductEventMessage.ProductData d) {
        return new Product(d.id(), d.name(), d.description(), d.price(), d.category(),
                Instant.parse(d.createdAt()), Instant.parse(d.updatedAt()));
    }
}
