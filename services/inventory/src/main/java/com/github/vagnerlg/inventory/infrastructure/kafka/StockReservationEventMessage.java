package com.github.vagnerlg.inventory.infrastructure.kafka;

record StockReservationEventMessage(String event, ReservationData data) {

    record ReservationData(String orderId, String reason) {
    }
}
