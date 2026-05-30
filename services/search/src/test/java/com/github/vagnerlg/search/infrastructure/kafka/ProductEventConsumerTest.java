package com.github.vagnerlg.search.infrastructure.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.vagnerlg.search.application.ProductSearchService;
import com.github.vagnerlg.search.domain.Product;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ProductEventConsumerTest {

    @Mock
    private ProductSearchService service;

    private ProductEventConsumer consumer;

    @BeforeEach
    void setUp() {
        var objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        consumer = new ProductEventConsumer(service, objectMapper);
    }

    @Test
    void shouldIndexOnCreatedEvent() {
        var record = record("CREATED");

        consumer.consume(record);

        verify(service).index(any(Product.class));
    }

    @Test
    void shouldIndexOnUpdatedEvent() {
        var record = record("UPDATED");

        consumer.consume(record);

        verify(service).index(any(Product.class));
    }

    @Test
    void shouldDeleteOnDeletedEvent() {
        var record = record("DELETED");

        consumer.consume(record);

        verify(service).delete(eq("abc-123"));
    }

    @Test
    void shouldIgnoreUnknownEvent() {
        var record = record("UNKNOWN");

        consumer.consume(record);

        verify(service, never()).index(any());
        verify(service, never()).delete(any());
    }

    @Test
    void shouldLogErrorOnMalformedMessage() {
        var record = new ConsumerRecord<>("product", 0, 0L, "abc-123", "not-json");

        consumer.consume(record);

        verify(service, never()).index(any());
        verify(service, never()).delete(any());
    }

    private ConsumerRecord<String, String> record(String eventType) {
        var now = Instant.now().toString();
        var json = """
                {
                  "event": "%s",
                  "data": {
                    "id": "abc-123",
                    "name": "Teclado Mecânico",
                    "description": "Switch Cherry MX Red",
                    "price": 349.90,
                    "category": "Periféricos",
                    "createdAt": "%s",
                    "updatedAt": "%s"
                  }
                }
                """.formatted(eventType, now, now);
        return new ConsumerRecord<>("product", 0, 0L, "abc-123", json);
    }
}
