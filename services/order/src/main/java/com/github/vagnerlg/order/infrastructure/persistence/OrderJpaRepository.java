package com.github.vagnerlg.order.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface OrderJpaRepository extends JpaRepository<OrderEntity, UUID> {
    Page<OrderEntity> findAllByUserId(String userId, Pageable pageable);
}
