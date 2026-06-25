package com.github.vagnerlg.inventory.application;

import com.github.vagnerlg.inventory.domain.Stock;
import com.github.vagnerlg.inventory.domain.StockRepository;
import com.github.vagnerlg.inventory.domain.StockReservationPublisher;
import com.github.vagnerlg.inventory.domain.exception.InsufficientStockException;
import com.github.vagnerlg.inventory.domain.exception.StockNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);
    private static final int INITIAL_STOCK = 10;

    private final StockRepository stockRepository;
    private final StockReservationPublisher reservationPublisher;

    public InventoryService(StockRepository stockRepository, StockReservationPublisher reservationPublisher) {
        this.stockRepository = stockRepository;
        this.reservationPublisher = reservationPublisher;
    }

    @Transactional
    public void initializeStock(String productId) {
        if (stockRepository.findByProductId(productId).isPresent()) {
            log.warn("Stock already exists for product: {}", productId);
            return;
        }
        stockRepository.save(new Stock(null, productId, INITIAL_STOCK, 0));
        log.info("Stock initialized for product: {} with quantity={}", productId, INITIAL_STOCK);
    }

    @Transactional
    public void reserveStock(String orderId, List<ReserveItem> items) {
        for (var item : items) {
            var stockOpt = stockRepository.findByProductId(item.productId());
            if (stockOpt.isEmpty()) {
                log.warn("Stock not found for order={} product={}", orderId, item.productId());
                reservationPublisher.publishUnavailable(orderId,
                        new StockNotFoundException(item.productId()).getMessage());
                return;
            }
            var stock = stockOpt.get();
            if (stock.availableQuantity() < item.quantity()) {
                log.warn("Insufficient stock for order={} product={} requested={} available={}",
                        orderId, item.productId(), item.quantity(), stock.availableQuantity());
                reservationPublisher.publishUnavailable(orderId,
                        new InsufficientStockException(item.productId(), item.quantity(), stock.availableQuantity()).getMessage());
                return;
            }
        }

        for (var item : items) {
            var stock = stockRepository.findByProductId(item.productId()).orElseThrow();
            stockRepository.save(new Stock(stock.id(), stock.productId(),
                    stock.totalQuantity(), stock.reservedQuantity() + item.quantity()));
        }

        reservationPublisher.publishReserved(orderId);
        log.info("Stock reserved for order={}", orderId);
    }

    @Transactional
    public void releaseStock(String orderId, List<ReserveItem> items) {
        for (var item : items) {
            stockRepository.findByProductId(item.productId()).ifPresent(stock -> {
                var newReserved = Math.max(0, stock.reservedQuantity() - item.quantity());
                stockRepository.save(new Stock(stock.id(), stock.productId(),
                        stock.totalQuantity(), newReserved));
            });
        }
        log.info("Stock released for order={}", orderId);
    }

    public record ReserveItem(String productId, int quantity) {
    }
}
