package com.github.vagnerlg.auth.infrastructure.kafka;

import com.github.vagnerlg.auth.domain.User;
import com.github.vagnerlg.auth.domain.exception.AuthEventPublishException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class KafkaUserEventPublisherTest {

    @SuppressWarnings("unchecked")
    private final KafkaTemplate<Object, Object> kafkaTemplate = Mockito.mock(KafkaTemplate.class);
    private final KafkaUserEventPublisher publisher = new KafkaUserEventPublisher(kafkaTemplate, "user");
    private final User user = new User("kc-id", "vagner", "Vagner", Instant.now());

    @AfterEach
    void clearInterruptedFlag() {
        Thread.interrupted();
    }

    @Test
    void publish_throwsAndSetsInterruptedFlagOnInterruptedException() throws Exception {
        @SuppressWarnings("unchecked")
        CompletableFuture<SendResult<Object, Object>> future = Mockito.mock(CompletableFuture.class);
        when(future.get()).thenThrow(new InterruptedException());
        when(kafkaTemplate.send(any(), any(), any())).thenReturn(future);

        assertThatThrownBy(() -> publisher.publish(user))
                .isInstanceOf(AuthEventPublishException.class);
        assertThat(Thread.currentThread().isInterrupted()).isTrue();
    }

    @Test
    void publish_throwsAuthEventPublishExceptionOnExecutionException() {
        CompletableFuture<SendResult<Object, Object>> future = new CompletableFuture<>();
        future.completeExceptionally(new RuntimeException("kafka broker unavailable"));
        when(kafkaTemplate.send(any(), any(), any())).thenReturn(future);

        assertThatThrownBy(() -> publisher.publish(user))
                .isInstanceOf(AuthEventPublishException.class);
    }
}
