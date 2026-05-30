package com.github.vagnerlg.product.infrastructure.persistence;

import org.springframework.data.mongodb.repository.MongoRepository;

interface MongoProductRepository extends MongoRepository<ProductDocument, String> {

    boolean existsByName(String name);
}
