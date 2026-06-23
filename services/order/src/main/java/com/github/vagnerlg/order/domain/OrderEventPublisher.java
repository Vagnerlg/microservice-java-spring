package com.github.vagnerlg.order.domain;

public interface OrderEventPublisher {
    void publishCreated(Order order);
    void publishCancelled(Order order);
}
