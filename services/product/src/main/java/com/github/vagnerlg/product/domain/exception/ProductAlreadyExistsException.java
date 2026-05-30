package com.github.vagnerlg.product.domain.exception;

public class ProductAlreadyExistsException extends RuntimeException {

    public ProductAlreadyExistsException(String name) {
        super("Product already exists with name: " + name);
    }
}
