package com.github.vagnerlg.inventory.infrastructure.kafka;

import com.github.vagnerlg.inventory.application.InventoryService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductEventConsumerTest {

    @Mock
    private InventoryService inventoryService;

    @Mock
    private ObjectMapper mapper;

    @InjectMocks
    private ProductEventConsumer consumer;

    @Test
    void consume_shouldInitializeStock_onCreatedEvent() throws Exception {
        var message = new ProductEventMessage("CREATED", new ProductEventMessage.ProductData("p-1"));
        when(mapper.readValue(any(String.class), eq(ProductEventMessage.class))).thenReturn(message);

        consumer.consume(new ConsumerRecord<>("product", 0, 0, "p-1", "{}"));

        verify(inventoryService).initializeStock("p-1");
    }

    @Test
    void consume_shouldIgnore_unknownEventType() throws Exception {
        var message = new ProductEventMessage("UPDATED", new ProductEventMessage.ProductData("p-1"));
        when(mapper.readValue(any(String.class), eq(ProductEventMessage.class))).thenReturn(message);

        consumer.consume(new ConsumerRecord<>("product", 0, 0, "p-1", "{}"));

        verify(inventoryService, never()).initializeStock(any());
    }

    @Test
    void consume_shouldNotThrow_onDeserializationError() throws Exception {
        when(mapper.readValue(any(String.class), eq(ProductEventMessage.class)))
                .thenThrow(new RuntimeException("bad json"));

        consumer.consume(new ConsumerRecord<>("product", 0, 0, "key", "bad"));
    }
}
