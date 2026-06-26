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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    private final ProductRepository productRepository;
    private final ProductEventPublisher eventPublisher;

    public ProductService(ProductRepository productRepository, ProductEventPublisher eventPublisher) {
        this.productRepository = productRepository;
        this.eventPublisher = eventPublisher;
    }

    public Product create(CreateProduct createProduct) {
        if (productRepository.existsByName(createProduct.name())) {
            throw new ProductAlreadyExistsException(createProduct.name());
        }
        var now = Instant.now();
        var product = new Product(null, createProduct.name(), createProduct.description(), createProduct.price(), createProduct.category(), now, now);
        var saved = productRepository.save(product);
        try {
            eventPublisher.publish(new ProductCreatedEvent(saved));
        } catch (ProductEventPublishException e) {
            try {
                productRepository.deleteById(saved.id());
            } catch (Exception deleteException) {
                log.error("Failed to delete product id={} after Kafka publish failure — manual reconciliation required", saved.id(), deleteException);
            }
            throw e;
        }
        return saved;
    }

    public Product findById(String id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
    }

    public Product update(UpdateProduct cmd) {
        var existing = productRepository.findById(cmd.id())
                .orElseThrow(() -> new ProductNotFoundException(cmd.id()));
        if (!existing.name().equals(cmd.name()) && productRepository.existsByName(cmd.name())) {
            throw new ProductAlreadyExistsException(cmd.name());
        }
        var updated = new Product(
                existing.id(),
                cmd.name(),
                cmd.description(),
                cmd.price(),
                cmd.category(),
                existing.createdAt(),
                Instant.now()
        );
        var saved = productRepository.save(updated);
        try {
            eventPublisher.publish(new ProductUpdatedEvent(saved));
        } catch (ProductEventPublishException e) {
            try {
                productRepository.save(existing);
            } catch (Exception restoreEx) {
                log.error("Failed to restore product id={} after Kafka publish failure — manual reconciliation required", saved.id(), restoreEx);
            }
            throw e;
        }
        return saved;
    }

    public void delete(String id) {
        var product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
        productRepository.deleteById(id);
        try {
            eventPublisher.publish(new ProductDeletedEvent(product));
        } catch (ProductEventPublishException e) {
            log.error("Failed to publish DELETED event for product id={} — search-service may still index it", id, e);
            throw e;
        }
    }
}
