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
class StockLevelEventConsumerTest {

    @Mock
    private ObjectMapper mapper;

    @InjectMocks
    private StockLevelEventConsumer consumer;

    @Test
    void consume_shouldLog_onLowEvent() throws Exception {
        var data = new StockLevelEventMessage.StockLevelData("p-1", 2);
        when(mapper.readValue(any(String.class), eq(StockLevelEventMessage.class)))
                .thenReturn(new StockLevelEventMessage("LOW", data));

        consumer.consume(new ConsumerRecord<>("stock-level", 0, 0, "p-1", "{}"));

        verify(mapper).readValue(any(String.class), eq(StockLevelEventMessage.class));
    }

    @Test
    void consume_shouldIgnore_unknownEventType() throws Exception {
        var data = new StockLevelEventMessage.StockLevelData("p-1", 2);
        when(mapper.readValue(any(String.class), eq(StockLevelEventMessage.class)))
                .thenReturn(new StockLevelEventMessage("UNKNOWN", data));

        consumer.consume(new ConsumerRecord<>("stock-level", 0, 0, "p-1", "{}"));

        verify(mapper).readValue(any(String.class), eq(StockLevelEventMessage.class));
    }

    @Test
    void consume_shouldNotThrow_onDeserializationError() throws Exception {
        when(mapper.readValue(any(String.class), eq(StockLevelEventMessage.class)))
                .thenThrow(new RuntimeException("bad json"));

        consumer.consume(new ConsumerRecord<>("stock-level", 0, 0, "key", "bad"));
    }
}
