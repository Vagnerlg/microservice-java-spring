package com.github.vagnerlg.inventory.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface StockJpaRepository extends JpaRepository<StockEntity, UUID> {
    Optional<StockEntity> findByProductId(String productId);
}
