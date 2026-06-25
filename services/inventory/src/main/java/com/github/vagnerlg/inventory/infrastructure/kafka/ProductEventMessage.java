package com.github.vagnerlg.inventory.infrastructure.kafka;

record ProductEventMessage(String event, ProductData data) {

    record ProductData(String id) {
    }
}
