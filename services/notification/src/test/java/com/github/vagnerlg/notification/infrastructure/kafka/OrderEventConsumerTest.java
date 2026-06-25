package com.github.vagnerlg.notification.infrastructure.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderEventConsumerTest {

    @Mock
    private ObjectMapper mapper;

    @InjectMocks
    private OrderEventConsumer consumer;

    @Test
    void consume_shouldLog_onCreatedEvent() throws Exception {
        var data = new OrderEventMessage.OrderData("order-1", "user-1",
                new java.math.BigDecimal("59.80"), java.time.Instant.now());
        when(mapper.readValue(any(String.class), eq(OrderEventMessage.class)))
                .thenReturn(new OrderEventMessage("CREATED", data));

        consumer.consume(new ConsumerRecord<>("order", 0, 0, "order-1", "{}"));

        verify(mapper).readValue(any(String.class), eq(OrderEventMessage.class));
    }

    @Test
    void consume_shouldLog_onCancelledEvent() throws Exception {
        var data = new OrderEventMessage.OrderData("order-1", "user-1",
                new java.math.BigDecimal("59.80"), java.time.Instant.now());
        when(mapper.readValue(any(String.class), eq(OrderEventMessage.class)))
                .thenReturn(new OrderEventMessage("CANCELLED", data));

        consumer.consume(new ConsumerRecord<>("order", 0, 0, "order-1", "{}"));

        verify(mapper).readValue(any(String.class), eq(OrderEventMessage.class));
    }

    @Test
    void consume_shouldIgnore_unknownEventType() throws Exception {
        var data = new OrderEventMessage.OrderData("order-1", "user-1",
                new java.math.BigDecimal("59.80"), java.time.Instant.now());
        when(mapper.readValue(any(String.class), eq(OrderEventMessage.class)))
                .thenReturn(new OrderEventMessage("UNKNOWN", data));

        consumer.consume(new ConsumerRecord<>("order", 0, 0, "order-1", "{}"));

        verify(mapper).readValue(any(String.class), eq(OrderEventMessage.class));
    }

    @Test
    void consume_shouldNotThrow_onDeserializationError() throws Exception {
        when(mapper.readValue(any(String.class), eq(OrderEventMessage.class)))
                .thenThrow(new RuntimeException("bad json"));

        consumer.consume(new ConsumerRecord<>("order", 0, 0, "key", "bad"));
    }
}
