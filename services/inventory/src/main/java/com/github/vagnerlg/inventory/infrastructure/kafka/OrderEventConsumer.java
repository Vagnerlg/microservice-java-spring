package com.github.vagnerlg.inventory.infrastructure.kafka;

import com.github.vagnerlg.inventory.application.InventoryService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

@Component
public class OrderEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderEventConsumer.class);

    private final InventoryService inventoryService;
    private final ObjectMapper mapper;

    public OrderEventConsumer(InventoryService inventoryService, ObjectMapper mapper) {
        this.inventoryService = inventoryService;
        this.mapper = mapper;
    }

    @KafkaListener(topics = "${kafka.topics.order}")
    public void consume(ConsumerRecord<String, String> record) {
        try {
            var message = mapper.readValue(record.value(), OrderEventMessage.class);
            var items = toReserveItems(message.data().items());
            switch (message.event()) {
                case "CREATED" -> inventoryService.reserveStock(message.data().orderId(), items);
                case "CANCELLED" -> inventoryService.releaseStock(message.data().orderId(), items);
                default -> log.warn("Ignoring unknown order event type: {}", message.event());
            }
        } catch (Exception e) {
            log.error("Failed to process order event key={}: {}", record.key(), e.getMessage());
        }
    }

    private List<InventoryService.ReserveItem> toReserveItems(List<OrderEventMessage.OrderItemData> items) {
        if (items == null) {
            return List.of();
        }
        return items.stream()
                .map(i -> new InventoryService.ReserveItem(i.productId(), i.quantity()))
                .toList();
    }
}
