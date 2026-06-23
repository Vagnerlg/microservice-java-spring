package com.github.vagnerlg.order.infrastructure.persistence;

import com.github.vagnerlg.order.domain.OrderStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders")
class OrderEntity {

    @Id
    UUID id;

    @Column(name = "user_id", nullable = false)
    String userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    OrderStatus status;

    @Column(name = "total_price", nullable = false)
    BigDecimal totalPrice;

    @Column(name = "cancellation_reason")
    String cancellationReason;

    @Column(name = "created_at", nullable = false)
    Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    Instant updatedAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    List<OrderItemEntity> items = new ArrayList<>();

    OrderEntity() {
    }

    OrderEntity(UUID id, String userId, OrderStatus status, BigDecimal totalPrice,
                String cancellationReason, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.userId = userId;
        this.status = status;
        this.totalPrice = totalPrice;
        this.cancellationReason = cancellationReason;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    void setItems(List<OrderItemEntity> items) {
        this.items = items;
    }

    UUID id() { return id; }
    String userId() { return userId; }
    OrderStatus status() { return status; }
    BigDecimal totalPrice() { return totalPrice; }
    String cancellationReason() { return cancellationReason; }
    Instant createdAt() { return createdAt; }
    Instant updatedAt() { return updatedAt; }
    List<OrderItemEntity> items() { return items; }
}
