package com.github.vagnerlg.user.infrastructure.kafka;

import com.github.vagnerlg.user.application.UserService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class UserEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(UserEventConsumer.class);

    private final UserService userService;
    private final ObjectMapper mapper;

    public UserEventConsumer(UserService userService, ObjectMapper mapper) {
        this.userService = userService;
        this.mapper = mapper;
    }

    @KafkaListener(topics = "${kafka.topics.user}")
    public void consume(ConsumerRecord<String, String> record) {
        try {
            var message = mapper.readValue(record.value(), UserEventMessage.class);
            if ("CREATED".equals(message.event())) {
                var data = message.data();
                userService.create(data.keycloakId(), data.username(), data.name(), data.createdAt());
            } else {
                log.warn("Ignoring unknown user event type: {}", message.event());
            }
        } catch (Exception e) {
            log.error("Failed to process user event key={}: {}", record.key(), e.getMessage());
        }
    }
}
