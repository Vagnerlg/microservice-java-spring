package com.github.vagnerlg.order.infrastructure.kafka;

import com.github.vagnerlg.order.application.OrderService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockReservationEventConsumerTest {

    @Mock
    private OrderService orderService;

    @Mock
    private ObjectMapper mapper;

    @InjectMocks
    private StockReservationEventConsumer consumer;

    @Test
    void consume_shouldConfirmOrder_onReservedEvent() throws Exception {
        var orderId = UUID.randomUUID();
        var data = new StockReservationEventMessage.ReservationData(orderId.toString(), null);
        var message = new StockReservationEventMessage("RESERVED", data);

        when(mapper.readValue(anyString(), eq(StockReservationEventMessage.class))).thenReturn(message);

        consumer.consume(new ConsumerRecord<>("stock-reservation", 0, 0, orderId.toString(), "{}"));

        verify(orderService).confirm(orderId);
        verify(orderService, never()).cancelBySystem(any(), any());
    }

    @Test
    void consume_shouldCancelOrder_onUnavailableEvent() throws Exception {
        var orderId = UUID.randomUUID();
        var data = new StockReservationEventMessage.ReservationData(orderId.toString(), "Out of stock");
        var message = new StockReservationEventMessage("UNAVAILABLE", data);

        when(mapper.readValue(anyString(), eq(StockReservationEventMessage.class))).thenReturn(message);

        consumer.consume(new ConsumerRecord<>("stock-reservation", 0, 0, orderId.toString(), "{}"));

        verify(orderService).cancelBySystem(orderId, "Out of stock");
        verify(orderService, never()).confirm(any());
    }

    @Test
    void consume_shouldIgnore_unknownEventType() throws Exception {
        var orderId = UUID.randomUUID();
        var data = new StockReservationEventMessage.ReservationData(orderId.toString(), null);
        var message = new StockReservationEventMessage("RELEASED", data);

        when(mapper.readValue(anyString(), eq(StockReservationEventMessage.class))).thenReturn(message);

        consumer.consume(new ConsumerRecord<>("stock-reservation", 0, 0, orderId.toString(), "{}"));

        verify(orderService, never()).confirm(any());
        verify(orderService, never()).cancelBySystem(any(), any());
    }

    @Test
    void consume_shouldNotThrow_onDeserializationError() throws Exception {
        when(mapper.readValue(anyString(), eq(StockReservationEventMessage.class)))
                .thenThrow(new RuntimeException("bad json"));

        consumer.consume(new ConsumerRecord<>("stock-reservation", 0, 0, "key", "bad"));
    }
}
