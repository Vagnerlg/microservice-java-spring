package com.github.vagnerlg.inventory.domain;

public interface StockReservationPublisher {
    void publishReserved(String orderId);
    void publishUnavailable(String orderId, String reason);
}
