package com.github.vagnerlg.notification.infrastructure.kafka;

record StockLevelEventMessage(String event, StockLevelData data) {

    record StockLevelData(String productId, int currentQuantity) {
    }
}
