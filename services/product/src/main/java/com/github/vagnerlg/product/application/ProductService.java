package com.github.vagnerlg.product.application;

import com.github.vagnerlg.product.domain.Product;
import com.github.vagnerlg.product.domain.ProductEventPublisher;
import com.github.vagnerlg.product.domain.ProductRepository;
import com.github.vagnerlg.product.domain.event.ProductCreatedEvent;
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
}
