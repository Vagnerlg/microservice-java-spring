package com.github.vagnerlg.product.application;

import com.github.vagnerlg.product.domain.Product;
import com.github.vagnerlg.product.domain.ProductEventPublisher;
import com.github.vagnerlg.product.domain.ProductRepository;
import com.github.vagnerlg.product.domain.event.ProductCreatedEvent;
import com.github.vagnerlg.product.domain.event.ProductDeletedEvent;
import com.github.vagnerlg.product.domain.event.ProductUpdatedEvent;
import com.github.vagnerlg.product.domain.exception.ProductAlreadyExistsException;
import com.github.vagnerlg.product.domain.exception.ProductEventPublishException;
import com.github.vagnerlg.product.domain.exception.ProductNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductEventPublisher eventPublisher;

    @InjectMocks
    private ProductService productService;

    // --- create ---

    @Test
    void create_shouldReturnSavedProduct() {
        var command = new CreateProduct("Notebook X", "High performance laptop", new BigDecimal("4999.99"), "Electronics");
        var saved = new Product("abc123", command.name(), command.description(), command.price(), command.category(), Instant.now(), Instant.now());

        when(productRepository.existsByName(command.name())).thenReturn(false);
        when(productRepository.save(any())).thenReturn(saved);

        var result = productService.create(command);

        assertThat(result.id()).isEqualTo("abc123");
        assertThat(result.name()).isEqualTo("Notebook X");
        verify(eventPublisher).publish(any(ProductCreatedEvent.class));
    }

    @Test
    void create_shouldThrow_whenNameAlreadyExists() {
        var command = new CreateProduct("Notebook X", "desc", BigDecimal.TEN, "Electronics");

        when(productRepository.existsByName(command.name())).thenReturn(true);

        assertThatThrownBy(() -> productService.create(command))
                .isInstanceOf(ProductAlreadyExistsException.class);
    }

    @Test
    void create_shouldDeleteProductAndRethrow_whenPublishFails() {
        var command = new CreateProduct("Notebook X", "desc", BigDecimal.TEN, "Electronics");
        var saved = new Product("abc123", command.name(), command.description(), command.price(), command.category(), Instant.now(), Instant.now());

        when(productRepository.existsByName(command.name())).thenReturn(false);
        when(productRepository.save(any())).thenReturn(saved);
        doThrow(new ProductEventPublishException("abc123", new RuntimeException("kafka down")))
                .when(eventPublisher).publish(any(ProductCreatedEvent.class));

        assertThatThrownBy(() -> productService.create(command))
                .isInstanceOf(ProductEventPublishException.class);
        verify(productRepository).deleteById("abc123");
    }

    @Test
    void create_shouldStillRethrowPublishException_whenDeleteAlsoFails() {
        var command = new CreateProduct("Notebook X", "desc", BigDecimal.TEN, "Electronics");
        var saved = new Product("abc123", command.name(), command.description(), command.price(), command.category(), Instant.now(), Instant.now());

        when(productRepository.existsByName(command.name())).thenReturn(false);
        when(productRepository.save(any())).thenReturn(saved);
        doThrow(new ProductEventPublishException("abc123", new RuntimeException("kafka down")))
                .when(eventPublisher).publish(any(ProductCreatedEvent.class));
        doThrow(new RuntimeException("mongo down")).when(productRepository).deleteById("abc123");

        assertThatThrownBy(() -> productService.create(command))
                .isInstanceOf(ProductEventPublishException.class);
    }

    // --- findById ---

    @Test
    void findById_shouldReturnProduct_whenExists() {
        var product = new Product("abc123", "Notebook X", "desc", BigDecimal.TEN, "Electronics", Instant.now(), Instant.now());

        when(productRepository.findById("abc123")).thenReturn(Optional.of(product));

        var result = productService.findById("abc123");

        assertThat(result.id()).isEqualTo("abc123");
    }

    @Test
    void findById_shouldThrow_whenNotFound() {
        when(productRepository.findById("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.findById("unknown"))
                .isInstanceOf(ProductNotFoundException.class);
    }

    // --- update ---

    @Test
    void update_shouldReturnUpdatedProduct() {
        var existing = new Product("abc", "Old Name", "old desc", BigDecimal.TEN, "Electronics", Instant.now(), Instant.now());
        var command = new UpdateProduct("abc", "New Name", "new desc", new BigDecimal("20.00"), "Tech");
        var saved = new Product("abc", "New Name", "new desc", new BigDecimal("20.00"), "Tech", existing.createdAt(), Instant.now());

        when(productRepository.findById("abc")).thenReturn(Optional.of(existing));
        when(productRepository.existsByName("New Name")).thenReturn(false);
        when(productRepository.save(any())).thenReturn(saved);

        var result = productService.update(command);

        assertThat(result.name()).isEqualTo("New Name");
        verify(eventPublisher).publish(any(ProductUpdatedEvent.class));
    }

    @Test
    void update_shouldThrow_whenProductNotFound() {
        when(productRepository.findById("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.update(new UpdateProduct("unknown", "N", "D", BigDecimal.TEN, "C")))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void update_shouldThrow_whenNameExistsOnAnotherProduct() {
        var existing = new Product("abc", "Old Name", "desc", BigDecimal.TEN, "Electronics", Instant.now(), Instant.now());

        when(productRepository.findById("abc")).thenReturn(Optional.of(existing));
        when(productRepository.existsByName("Other Product")).thenReturn(true);

        assertThatThrownBy(() -> productService.update(new UpdateProduct("abc", "Other Product", "desc", BigDecimal.TEN, "Electronics")))
                .isInstanceOf(ProductAlreadyExistsException.class);
    }

    @Test
    void update_shouldNotCheckNameConflict_whenNameUnchanged() {
        var existing = new Product("abc", "Same Name", "desc", BigDecimal.TEN, "Electronics", Instant.now(), Instant.now());

        when(productRepository.findById("abc")).thenReturn(Optional.of(existing));
        when(productRepository.save(any())).thenReturn(existing);

        productService.update(new UpdateProduct("abc", "Same Name", "new desc", BigDecimal.TEN, "Electronics"));

        verify(productRepository, never()).existsByName(any());
    }

    @Test
    void update_shouldRestoreOldStateAndRethrow_whenPublishFails() {
        var existing = new Product("abc", "Old Name", "desc", BigDecimal.TEN, "Electronics", Instant.now(), Instant.now());
        var saved = new Product("abc", "New Name", "desc", BigDecimal.TEN, "Electronics", existing.createdAt(), Instant.now());
        var command = new UpdateProduct("abc", "New Name", "desc", BigDecimal.TEN, "Electronics");

        when(productRepository.findById("abc")).thenReturn(Optional.of(existing));
        when(productRepository.existsByName("New Name")).thenReturn(false);
        when(productRepository.save(any())).thenReturn(saved);
        doThrow(new ProductEventPublishException("abc", new RuntimeException("kafka down")))
                .when(eventPublisher).publish(any(ProductUpdatedEvent.class));

        assertThatThrownBy(() -> productService.update(command))
                .isInstanceOf(ProductEventPublishException.class);
        verify(productRepository, times(2)).save(any());
    }

    @Test
    void update_shouldStillRethrow_whenRestoreAlsoFails() {
        var existing = new Product("abc", "Old Name", "desc", BigDecimal.TEN, "Electronics", Instant.now(), Instant.now());
        var saved = new Product("abc", "New Name", "desc", BigDecimal.TEN, "Electronics", existing.createdAt(), Instant.now());
        var command = new UpdateProduct("abc", "New Name", "desc", BigDecimal.TEN, "Electronics");

        when(productRepository.findById("abc")).thenReturn(Optional.of(existing));
        when(productRepository.existsByName("New Name")).thenReturn(false);
        when(productRepository.save(any()))
                .thenReturn(saved)
                .thenThrow(new RuntimeException("mongo down"));
        doThrow(new ProductEventPublishException("abc", new RuntimeException("kafka down")))
                .when(eventPublisher).publish(any(ProductUpdatedEvent.class));

        assertThatThrownBy(() -> productService.update(command))
                .isInstanceOf(ProductEventPublishException.class);
    }

    // --- delete ---

    @Test
    void delete_shouldDeleteAndPublishEvent() {
        var product = new Product("abc", "Name", "desc", BigDecimal.TEN, "Electronics", Instant.now(), Instant.now());

        when(productRepository.findById("abc")).thenReturn(Optional.of(product));

        productService.delete("abc");

        verify(productRepository).deleteById("abc");
        verify(eventPublisher).publish(any(ProductDeletedEvent.class));
    }

    @Test
    void delete_shouldThrow_whenProductNotFound() {
        when(productRepository.findById("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.delete("unknown"))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void delete_shouldRethrow_whenPublishFails() {
        var product = new Product("abc", "Name", "desc", BigDecimal.TEN, "Electronics", Instant.now(), Instant.now());

        when(productRepository.findById("abc")).thenReturn(Optional.of(product));
        doThrow(new ProductEventPublishException("abc", new RuntimeException("kafka down")))
                .when(eventPublisher).publish(any(ProductDeletedEvent.class));

        assertThatThrownBy(() -> productService.delete("abc"))
                .isInstanceOf(ProductEventPublishException.class);
        verify(productRepository).deleteById("abc");
    }
}
