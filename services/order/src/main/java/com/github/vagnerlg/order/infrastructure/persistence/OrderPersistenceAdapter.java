package com.github.vagnerlg.order.infrastructure.persistence;

import com.github.vagnerlg.order.domain.Order;
import com.github.vagnerlg.order.domain.OrderItem;
import com.github.vagnerlg.order.domain.OrderRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
class OrderPersistenceAdapter implements OrderRepository {

    private final OrderJpaRepository jpaRepository;

    OrderPersistenceAdapter(OrderJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Order save(Order order) {
        var entity = toEntity(order);
        return toDomain(jpaRepository.save(entity));
    }

    @Override
    public Optional<Order> findById(UUID id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Page<Order> findAllByUserId(String userId, Pageable pageable) {
        return jpaRepository.findAllByUserId(userId, pageable).map(this::toDomain);
    }

    private OrderEntity toEntity(Order order) {
        var entity = new OrderEntity(
                order.id(), order.userId(), order.status(),
                order.totalPrice(), order.cancellationReason(),
                order.createdAt(), order.updatedAt()
        );
        var itemEntities = order.items().stream()
                .map(i -> new OrderItemEntity(i.id(), entity, i.productId(), i.name(), i.price(), i.quantity()))
                .toList();
        entity.setItems(itemEntities);
        return entity;
    }

    private Order toDomain(OrderEntity entity) {
        var items = entity.items().stream()
                .map(i -> new OrderItem(i.id(), i.productId(), i.name(), i.price(), i.quantity()))
                .toList();
        return new Order(
                entity.id(), entity.userId(), items, entity.status(),
                entity.totalPrice(), entity.cancellationReason(),
                entity.createdAt(), entity.updatedAt()
        );
    }
}
