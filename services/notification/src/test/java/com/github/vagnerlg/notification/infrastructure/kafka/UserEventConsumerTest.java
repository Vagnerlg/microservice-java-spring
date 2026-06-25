package com.github.vagnerlg.notification.infrastructure.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserEventConsumerTest {

    @Mock
    private ObjectMapper mapper;

    @InjectMocks
    private UserEventConsumer consumer;

    @Test
    void consume_shouldLog_onCreatedEvent() throws Exception {
        var data = new UserEventMessage.UserData("kc-id-1", "joao", "João Silva", Instant.now());
        when(mapper.readValue(any(String.class), eq(UserEventMessage.class)))
                .thenReturn(new UserEventMessage("CREATED", data));

        consumer.consume(new ConsumerRecord<>("user", 0, 0, "kc-id-1", "{}"));

        verify(mapper).readValue(any(String.class), eq(UserEventMessage.class));
    }

    @Test
    void consume_shouldIgnore_unknownEventType() throws Exception {
        var data = new UserEventMessage.UserData("kc-id-1", "joao", "João Silva", Instant.now());
        when(mapper.readValue(any(String.class), eq(UserEventMessage.class)))
                .thenReturn(new UserEventMessage("UNKNOWN", data));

        consumer.consume(new ConsumerRecord<>("user", 0, 0, "kc-id-1", "{}"));

        verify(mapper).readValue(any(String.class), eq(UserEventMessage.class));
    }

    @Test
    void consume_shouldNotThrow_onDeserializationError() throws Exception {
        when(mapper.readValue(any(String.class), eq(UserEventMessage.class)))
                .thenThrow(new RuntimeException("bad json"));

        consumer.consume(new ConsumerRecord<>("user", 0, 0, "key", "bad"));
    }
}
