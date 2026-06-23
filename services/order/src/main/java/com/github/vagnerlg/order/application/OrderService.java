package com.github.vagnerlg.order.application;

import com.github.vagnerlg.order.domain.CreateOrderItem;
import com.github.vagnerlg.order.domain.Order;
import com.github.vagnerlg.order.domain.OrderEventPublisher;
import com.github.vagnerlg.order.domain.OrderItem;
import com.github.vagnerlg.order.domain.OrderRepository;
import com.github.vagnerlg.order.domain.OrderStatus;
import com.github.vagnerlg.order.domain.exception.OrderAccessDeniedException;
import com.github.vagnerlg.order.domain.exception.OrderCancellationException;
import com.github.vagnerlg.order.domain.exception.OrderNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderEventPublisher orderEventPublisher;

    public OrderService(OrderRepository orderRepository, OrderEventPublisher orderEventPublisher) {
        this.orderRepository = orderRepository;
        this.orderEventPublisher = orderEventPublisher;
    }

    @Transactional
    public Order createFromCheckout(String userId, List<CreateOrderItem> items) {
        var orderId = UUID.randomUUID();
        var now = Instant.now();
        var orderItems = items.stream()
                .map(i -> new OrderItem(UUID.randomUUID(), i.productId(), i.name(), i.price(), i.quantity()))
                .toList();
        var totalPrice = orderItems.stream()
                .map(i -> i.price().multiply(BigDecimal.valueOf(i.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        var order = new Order(orderId, userId, orderItems, OrderStatus.PENDING, totalPrice, null, now, now);
        var saved = orderRepository.save(order);
        orderEventPublisher.publishCreated(saved);
        return saved;
    }

    public Page<Order> listByUser(String userId, Pageable pageable) {
        return orderRepository.findAllByUserId(userId, pageable);
    }

    public Order findById(UUID id, String userId) {
        var order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));
        if (!order.userId().equals(userId)) {
            throw new OrderAccessDeniedException();
        }
        return order;
    }

    @Transactional
    public void confirm(UUID orderId) {
        var order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        var updated = new Order(order.id(), order.userId(), order.items(), OrderStatus.CONFIRMED,
                order.totalPrice(), order.cancellationReason(), order.createdAt(), Instant.now());
        orderRepository.save(updated);
    }

    @Transactional
    public void cancelBySystem(UUID orderId, String reason) {
        var order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        var updated = new Order(order.id(), order.userId(), order.items(), OrderStatus.CANCELLED,
                order.totalPrice(), reason, order.createdAt(), Instant.now());
        var saved = orderRepository.save(updated);
        orderEventPublisher.publishCancelled(saved);
    }

    @Transactional
    public void cancelByUser(UUID orderId, String userId) {
        var order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        if (!order.userId().equals(userId)) {
            throw new OrderAccessDeniedException();
        }
        if (order.status() != OrderStatus.PENDING) {
            throw new OrderCancellationException(order.status().name());
        }
        var updated = new Order(order.id(), order.userId(), order.items(), OrderStatus.CANCELLED,
                order.totalPrice(), "Cancelled by user", order.createdAt(), Instant.now());
        var saved = orderRepository.save(updated);
        orderEventPublisher.publishCancelled(saved);
    }
}
