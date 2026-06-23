package com.github.vagnerlg.order.infrastructure.kafka;

import com.github.vagnerlg.order.application.OrderService;
import com.github.vagnerlg.order.domain.CreateOrderItem;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartEventConsumerTest {

    @Mock
    private OrderService orderService;

    @Mock
    private ObjectMapper mapper;

    @InjectMocks
    private CartEventConsumer cartEventConsumer;

    @Test
    void consume_shouldCreateOrder_onCheckoutEvent() throws Exception {
        var itemData = new CartEventMessage.CartItemData("p-1", "Widget", new java.math.BigDecimal("10.00"), 2);
        var data = new CartEventMessage.CartData("user-1", List.of(itemData), java.time.Instant.now());
        var message = new CartEventMessage("CHECKOUT", data);

        when(mapper.readValue(any(String.class), eq(CartEventMessage.class))).thenReturn(message);

        cartEventConsumer.consume(new ConsumerRecord<>("cart", 0, 0, "user-1", "{}"));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<CreateOrderItem>> captor = ArgumentCaptor.forClass(List.class);
        verify(orderService).createFromCheckout(eq("user-1"), captor.capture());
        assertThat(captor.getValue()).hasSize(1);
        assertThat(captor.getValue().get(0).productId()).isEqualTo("p-1");
    }

    @Test
    void consume_shouldIgnore_unknownEventType() throws Exception {
        var data = new CartEventMessage.CartData("user-1", List.of(), java.time.Instant.now());
        var message = new CartEventMessage("UNKNOWN", data);

        when(mapper.readValue(any(String.class), eq(CartEventMessage.class))).thenReturn(message);

        cartEventConsumer.consume(new ConsumerRecord<>("cart", 0, 0, "user-1", "{}"));

        verify(orderService, never()).createFromCheckout(any(), anyList());
    }

    @Test
    void consume_shouldNotThrow_onDeserializationError() throws Exception {
        when(mapper.readValue(any(String.class), eq(CartEventMessage.class)))
                .thenThrow(new RuntimeException("bad json"));

        cartEventConsumer.consume(new ConsumerRecord<>("cart", 0, 0, "key", "bad"));
    }
}
