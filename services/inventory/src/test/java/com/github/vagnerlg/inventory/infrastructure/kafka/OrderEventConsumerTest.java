package com.github.vagnerlg.inventory.infrastructure.kafka;

import com.github.vagnerlg.inventory.application.InventoryService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderEventConsumerTest {

    @Mock
    private InventoryService inventoryService;

    @Mock
    private ObjectMapper mapper;

    @InjectMocks
    private OrderEventConsumer consumer;

    @Test
    void consume_shouldReserveStock_onCreatedEvent() throws Exception {
        var items = List.of(new OrderEventMessage.OrderItemData("p-1", 2));
        var data = new OrderEventMessage.OrderData("order-1", "user-1", items);
        var message = new OrderEventMessage("CREATED", data);
        when(mapper.readValue(any(String.class), eq(OrderEventMessage.class))).thenReturn(message);

        consumer.consume(new ConsumerRecord<>("order", 0, 0, "order-1", "{}"));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<InventoryService.ReserveItem>> captor = ArgumentCaptor.forClass(List.class);
        verify(inventoryService).reserveStock(eq("order-1"), captor.capture());
        assertThat(captor.getValue()).hasSize(1);
        assertThat(captor.getValue().get(0).productId()).isEqualTo("p-1");
        assertThat(captor.getValue().get(0).quantity()).isEqualTo(2);
    }

    @Test
    void consume_shouldReleaseStock_onCancelledEvent() throws Exception {
        var items = List.of(new OrderEventMessage.OrderItemData("p-1", 2));
        var data = new OrderEventMessage.OrderData("order-1", "user-1", items);
        var message = new OrderEventMessage("CANCELLED", data);
        when(mapper.readValue(any(String.class), eq(OrderEventMessage.class))).thenReturn(message);

        consumer.consume(new ConsumerRecord<>("order", 0, 0, "order-1", "{}"));

        verify(inventoryService).releaseStock(eq("order-1"), anyList());
    }

    @Test
    void consume_shouldIgnore_unknownEventType() throws Exception {
        var data = new OrderEventMessage.OrderData("order-1", "user-1", List.of());
        var message = new OrderEventMessage("UNKNOWN", data);
        when(mapper.readValue(any(String.class), eq(OrderEventMessage.class))).thenReturn(message);

        consumer.consume(new ConsumerRecord<>("order", 0, 0, "order-1", "{}"));

        verify(inventoryService, never()).reserveStock(anyString(), anyList());
        verify(inventoryService, never()).releaseStock(anyString(), anyList());
    }

    @Test
    void consume_shouldNotThrow_onDeserializationError() throws Exception {
        when(mapper.readValue(any(String.class), eq(OrderEventMessage.class)))
                .thenThrow(new RuntimeException("bad json"));

        consumer.consume(new ConsumerRecord<>("order", 0, 0, "key", "bad"));
    }
}
