package com.github.vagnerlg.notification.infrastructure.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class UserEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(UserEventConsumer.class);

    private final ObjectMapper mapper;

    public UserEventConsumer(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @KafkaListener(topics = "${kafka.topics.user}")
    public void consume(ConsumerRecord<String, String> record) {
        try {
            var message = mapper.readValue(record.value(), UserEventMessage.class);
            if ("CREATED".equals(message.event())) {
                log.info("[notification] user.CREATED username={} name={}",
                        message.data().username(), message.data().name());
            } else {
                log.warn("[notification] user event ignored type={}", message.event());
            }
        } catch (Exception e) {
            log.error("[notification] failed to process user event key={}: {}", record.key(), e.getMessage());
        }
    }
}
