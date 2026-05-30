package com.github.vagnerlg.search.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductRepository {
    void save(Product product);
    void deleteById(String id);
    Page<Product> search(String query, String category, Pageable pageable);
}
