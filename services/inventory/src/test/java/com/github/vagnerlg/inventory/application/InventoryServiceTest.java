package com.github.vagnerlg.inventory.application;

import com.github.vagnerlg.inventory.domain.Stock;
import com.github.vagnerlg.inventory.domain.StockRepository;
import com.github.vagnerlg.inventory.domain.StockReservationPublisher;
import com.github.vagnerlg.inventory.domain.exception.StockNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private StockRepository stockRepository;

    @Mock
    private StockReservationPublisher reservationPublisher;

    @InjectMocks
    private InventoryService inventoryService;

    @Test
    void initializeStock_shouldSaveWithInitialQuantity() {
        when(stockRepository.findByProductId("p-1")).thenReturn(Optional.empty());
        when(stockRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        inventoryService.initializeStock("p-1");

        var captor = ArgumentCaptor.forClass(Stock.class);
        verify(stockRepository).save(captor.capture());
        assertThat(captor.getValue().productId()).isEqualTo("p-1");
        assertThat(captor.getValue().totalQuantity()).isEqualTo(10);
        assertThat(captor.getValue().reservedQuantity()).isEqualTo(0);
    }

    @Test
    void initializeStock_shouldBeIdempotent_whenStockAlreadyExists() {
        var existing = new Stock(UUID.randomUUID(), "p-1", 10, 0);
        when(stockRepository.findByProductId("p-1")).thenReturn(Optional.of(existing));

        inventoryService.initializeStock("p-1");

        verify(stockRepository, never()).save(any());
    }

    @Test
    void reserveStock_shouldIncrementReservedQuantityAndPublishReserved() {
        var stock = new Stock(UUID.randomUUID(), "p-1", 10, 0);
        when(stockRepository.findByProductId("p-1")).thenReturn(Optional.of(stock));
        when(stockRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        inventoryService.reserveStock("order-1", List.of(new InventoryService.ReserveItem("p-1", 3)));

        var captor = ArgumentCaptor.forClass(Stock.class);
        verify(stockRepository).save(captor.capture());
        assertThat(captor.getValue().reservedQuantity()).isEqualTo(3);
        verify(reservationPublisher).publishReserved("order-1");
    }

    @Test
    void reserveStock_shouldPublishUnavailable_whenInsufficientStock() {
        var stock = new Stock(UUID.randomUUID(), "p-1", 2, 0);
        when(stockRepository.findByProductId("p-1")).thenReturn(Optional.of(stock));

        inventoryService.reserveStock("order-1", List.of(new InventoryService.ReserveItem("p-1", 5)));

        verify(stockRepository, never()).save(any());
        verify(reservationPublisher).publishUnavailable(anyString(), contains("p-1"));
        verify(reservationPublisher, never()).publishReserved(anyString());
    }

    @Test
    void reserveStock_shouldPublishUnavailable_whenStockNotFound() {
        when(stockRepository.findByProductId("p-99")).thenReturn(Optional.empty());

        inventoryService.reserveStock("order-1", List.of(new InventoryService.ReserveItem("p-99", 1)));

        verify(reservationPublisher).publishUnavailable(anyString(), contains("p-99"));
    }

    @Test
    void releaseStock_shouldDecrementReservedQuantity() {
        var stock = new Stock(UUID.randomUUID(), "p-1", 10, 5);
        when(stockRepository.findByProductId("p-1")).thenReturn(Optional.of(stock));
        when(stockRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        inventoryService.releaseStock("order-1", List.of(new InventoryService.ReserveItem("p-1", 3)));

        var captor = ArgumentCaptor.forClass(Stock.class);
        verify(stockRepository).save(captor.capture());
        assertThat(captor.getValue().reservedQuantity()).isEqualTo(2);
    }

    @Test
    void releaseStock_shouldNotGoBelowZero_whenReleaseExceedsReserved() {
        var stock = new Stock(UUID.randomUUID(), "p-1", 10, 1);
        when(stockRepository.findByProductId("p-1")).thenReturn(Optional.of(stock));
        when(stockRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        inventoryService.releaseStock("order-1", List.of(new InventoryService.ReserveItem("p-1", 5)));

        var captor = ArgumentCaptor.forClass(Stock.class);
        verify(stockRepository).save(captor.capture());
        assertThat(captor.getValue().reservedQuantity()).isEqualTo(0);
    }

    @Test
    void releaseStock_shouldIgnore_whenStockNotFound() {
        when(stockRepository.findByProductId("p-99")).thenReturn(Optional.empty());

        inventoryService.releaseStock("order-1", List.of(new InventoryService.ReserveItem("p-99", 1)));

        verify(stockRepository, never()).save(any());
    }
}
