package com.github.vagnerlg.inventory.infrastructure.kafka;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KafkaStockReservationPublisherTest {

    @Mock
    private KafkaTemplate<Object, Object> kafkaTemplate;

    private KafkaStockReservationPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new KafkaStockReservationPublisher(kafkaTemplate, "stock-reservation");
    }

    @Test
    void publishReserved_shouldSendReservedEvent() {
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(completedFuture());

        publisher.publishReserved("order-1");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<StockReservationEventMessage> captor =
                ArgumentCaptor.forClass(StockReservationEventMessage.class);
        verify(kafkaTemplate).send(anyString(), anyString(), captor.capture());
        assertThat(captor.getValue().event()).isEqualTo("RESERVED");
        assertThat(captor.getValue().data().orderId()).isEqualTo("order-1");
        assertThat(captor.getValue().data().reason()).isNull();
    }

    @Test
    void publishUnavailable_shouldSendUnavailableEventWithReason() {
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(completedFuture());

        publisher.publishUnavailable("order-1", "Out of stock");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<StockReservationEventMessage> captor =
                ArgumentCaptor.forClass(StockReservationEventMessage.class);
        verify(kafkaTemplate).send(anyString(), anyString(), captor.capture());
        assertThat(captor.getValue().event()).isEqualTo("UNAVAILABLE");
        assertThat(captor.getValue().data().reason()).isEqualTo("Out of stock");
    }

    @Test
    void publishReserved_shouldThrow_onExecutionException() {
        var failedFuture = new CompletableFuture<SendResult<Object, Object>>();
        failedFuture.completeExceptionally(new RuntimeException("Kafka error"));
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(failedFuture);

        assertThatThrownBy(() -> publisher.publishReserved("order-1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Failed to publish");
    }

    @SuppressWarnings("unchecked")
    private CompletableFuture<SendResult<Object, Object>> completedFuture() {
        return CompletableFuture.completedFuture(
                new SendResult<>(new ProducerRecord<>("stock-reservation", "order-1", null),
                        new RecordMetadata(null, 0, 0, 0, 0, 0)));
    }
}
