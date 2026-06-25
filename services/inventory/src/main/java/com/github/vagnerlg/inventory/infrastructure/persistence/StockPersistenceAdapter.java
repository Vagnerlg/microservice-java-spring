package com.github.vagnerlg.inventory.infrastructure.persistence;

import com.github.vagnerlg.inventory.domain.Stock;
import com.github.vagnerlg.inventory.domain.StockRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
class StockPersistenceAdapter implements StockRepository {

    private final StockJpaRepository jpaRepository;

    StockPersistenceAdapter(StockJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Stock save(Stock stock) {
        var entity = new StockEntity(stock.id(), stock.productId(),
                stock.totalQuantity(), stock.reservedQuantity());
        return toDomain(jpaRepository.save(entity));
    }

    @Override
    public Optional<Stock> findByProductId(String productId) {
        return jpaRepository.findByProductId(productId).map(this::toDomain);
    }

    private Stock toDomain(StockEntity e) {
        return new Stock(e.id(), e.productId(), e.totalQuantity(), e.reservedQuantity());
    }
}
