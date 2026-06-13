package com.github.vagnerlg.user.infrastructure.kafka;

import com.github.vagnerlg.user.application.UserService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserEventConsumerTest {

    @Mock
    private UserService userService;

    private final ObjectMapper mapper = new ObjectMapper();

    private UserEventConsumer consumer() {
        return new UserEventConsumer(userService, mapper);
    }

    @Test
    void consume_shouldCreateUser_whenEventIsCreated() {
        var json = """
                {"event":"CREATED","data":{"keycloakId":"kc-1","username":"john","name":"John Doe","createdAt":"2024-01-01T00:00:00Z"}}
                """;
        consumer().consume(new ConsumerRecord<>("user", 0, 0L, "kc-1", json));

        verify(userService).create(eq("kc-1"), eq("john"), eq("John Doe"), any(Instant.class));
    }

    @Test
    void consume_shouldIgnore_whenEventTypeIsUnknown() {
        var json = """
                {"event":"UPDATED","data":{"keycloakId":"kc-1","username":"john","name":"John Doe","createdAt":"2024-01-01T00:00:00Z"}}
                """;
        consumer().consume(new ConsumerRecord<>("user", 0, 0L, "kc-1", json));

        verify(userService, never()).create(any(), any(), any(), any());
    }

    @Test
    void consume_shouldNotThrow_whenJsonIsInvalid() {
        consumer().consume(new ConsumerRecord<>("user", 0, 0L, "key", "not-valid-json"));

        verify(userService, never()).create(any(), any(), any(), any());
    }
}
