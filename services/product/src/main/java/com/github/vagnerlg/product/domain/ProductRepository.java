package com.github.vagnerlg.product.domain;

import java.util.Optional;

public interface ProductRepository {

    Product save(Product product);

    Optional<Product> findById(String id);

    boolean existsByName(String name);

    void deleteById(String id);
}
