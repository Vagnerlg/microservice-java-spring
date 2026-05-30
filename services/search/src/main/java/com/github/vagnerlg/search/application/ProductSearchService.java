package com.github.vagnerlg.search.application;

import com.github.vagnerlg.search.domain.Product;
import com.github.vagnerlg.search.domain.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class ProductSearchService {

    private final ProductRepository repository;

    public ProductSearchService(ProductRepository repository) {
        this.repository = repository;
    }

    public void index(Product product) {
        repository.save(product);
    }

    public void delete(String id) {
        repository.deleteById(id);
    }

    public Page<Product> search(String query, String category, Pageable pageable) {
        return repository.search(query, category, pageable);
    }
}
