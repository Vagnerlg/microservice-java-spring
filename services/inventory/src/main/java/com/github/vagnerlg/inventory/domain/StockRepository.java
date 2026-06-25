package com.github.vagnerlg.inventory.domain;

import java.util.Optional;

public interface StockRepository {
    Stock save(Stock stock);
    Optional<Stock> findByProductId(String productId);
}
