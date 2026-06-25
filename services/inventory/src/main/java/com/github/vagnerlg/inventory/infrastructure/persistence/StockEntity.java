package com.github.vagnerlg.inventory.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "stock")
class StockEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @Column(name = "product_id", nullable = false, unique = true)
    String productId;

    @Column(name = "total_quantity", nullable = false)
    int totalQuantity;

    @Column(name = "reserved_quantity", nullable = false)
    int reservedQuantity;

    @Column(name = "created_at", nullable = false, updatable = false)
    Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    Instant updatedAt;

    StockEntity() {
    }

    StockEntity(UUID id, String productId, int totalQuantity, int reservedQuantity) {
        this.id = id;
        this.productId = productId;
        this.totalQuantity = totalQuantity;
        this.reservedQuantity = reservedQuantity;
    }

    @PrePersist
    void onPersist() {
        var now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    UUID id() { return id; }
    String productId() { return productId; }
    int totalQuantity() { return totalQuantity; }
    int reservedQuantity() { return reservedQuantity; }
    Instant createdAt() { return createdAt; }
    Instant updatedAt() { return updatedAt; }
}
