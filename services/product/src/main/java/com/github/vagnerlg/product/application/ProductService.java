package com.github.vagnerlg.product.application;

import com.github.vagnerlg.product.domain.Product;
import com.github.vagnerlg.product.domain.ProductRepository;
import com.github.vagnerlg.product.domain.exception.ProductAlreadyExistsException;
import com.github.vagnerlg.product.domain.exception.ProductNotFoundException;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public Product create(CreateProduct createProduct) {
        if (productRepository.existsByName(createProduct.name())) {
            throw new ProductAlreadyExistsException(createProduct.name());
        }
        var now = Instant.now();
        var product = new Product(null, createProduct.name(), createProduct.description(), createProduct.price(), createProduct.category(), now, now);
        return productRepository.save(product);
    }

    public Product findById(String id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
    }
}
