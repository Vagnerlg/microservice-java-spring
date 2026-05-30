package com.github.vagnerlg.product.infrastructure.persistence;

import com.github.vagnerlg.product.domain.ProductRepository;
import com.github.vagnerlg.product.domain.Product;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
class ProductPersistenceAdapter implements ProductRepository {

    private final MongoProductRepository mongoRepository;

    ProductPersistenceAdapter(MongoProductRepository mongoRepository) {
        this.mongoRepository = mongoRepository;
    }

    @Override
    public Product save(Product product) {
        var doc = toDocument(product);
        var saved = mongoRepository.save(doc);
        return toDomain(saved);
    }

    @Override
    public Optional<Product> findById(String id) {
        return mongoRepository.findById(id).map(this::toDomain);
    }

    @Override
    public boolean existsByName(String name) {
        return mongoRepository.existsByName(name);
    }

    private ProductDocument toDocument(Product product) {
        return new ProductDocument(
                product.id(),
                product.name(),
                product.description(),
                product.price(),
                product.category(),
                product.createdAt(),
                product.updatedAt()
        );
    }

    private Product toDomain(ProductDocument doc) {
        return new Product(
                doc.id(),
                doc.name(),
                doc.description(),
                doc.price(),
                doc.category(),
                doc.createdAt(),
                doc.updatedAt()
        );
    }
}
