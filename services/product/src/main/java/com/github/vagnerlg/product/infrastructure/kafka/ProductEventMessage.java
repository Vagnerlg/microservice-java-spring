package com.github.vagnerlg.product.infrastructure.kafka;

import com.github.vagnerlg.product.domain.Product;

record ProductEventMessage(String event, Product data) {
}
