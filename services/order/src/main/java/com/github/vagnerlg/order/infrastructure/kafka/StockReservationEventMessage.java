package com.github.vagnerlg.order.infrastructure.kafka;

record StockReservationEventMessage(String event, ReservationData data) {

    record ReservationData(String orderId, String reason) {
    }
}
