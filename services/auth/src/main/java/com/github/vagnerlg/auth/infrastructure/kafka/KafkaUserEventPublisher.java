package com.github.vagnerlg.auth.infrastructure.kafka;

import com.github.vagnerlg.auth.domain.User;
import com.github.vagnerlg.auth.domain.exception.AuthEventPublishException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;

@Component
public class KafkaUserEventPublisher {

    private final KafkaTemplate<Object, Object> kafkaTemplate;
    private final String topic;

    KafkaUserEventPublisher(KafkaTemplate<Object, Object> kafkaTemplate,
                            @Value("${kafka.topics.user}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    public void publish(User user) {
        var message = UserEventMessage.created(user);
        try {
            kafkaTemplate.send(topic, user.keycloakId(), message).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AuthEventPublishException(user.username(), e);
        } catch (ExecutionException e) {
            throw new AuthEventPublishException(user.username(), e.getCause());
        }
    }
}
