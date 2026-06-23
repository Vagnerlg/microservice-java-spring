package com.github.vagnerlg.order.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "order_items")
class OrderItemEntity {

    @Id
    UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    OrderEntity order;

    @Column(name = "product_id", nullable = false)
    String productId;

    @Column(nullable = false)
    String name;

    @Column(nullable = false)
    BigDecimal price;

    @Column(nullable = false)
    int quantity;

    OrderItemEntity() {
    }

    OrderItemEntity(UUID id, OrderEntity order, String productId, String name, BigDecimal price, int quantity) {
        this.id = id;
        this.order = order;
        this.productId = productId;
        this.name = name;
        this.price = price;
        this.quantity = quantity;
    }

    UUID id() { return id; }
    String productId() { return productId; }
    String name() { return name; }
    BigDecimal price() { return price; }
    int quantity() { return quantity; }
}
