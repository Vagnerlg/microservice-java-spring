package com.github.vagnerlg.product.application;

import com.github.vagnerlg.product.domain.Product;
import com.github.vagnerlg.product.domain.ProductEventPublisher;
import com.github.vagnerlg.product.domain.ProductRepository;
import com.github.vagnerlg.product.domain.event.ProductCreatedEvent;
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
                .when(eventPublisher).publish(any());

        assertThatThrownBy(() -> productService.create(command))
                .isInstanceOf(ProductEventPublishException.class);
        verify(productRepository).deleteById("abc123");
    }

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
}
