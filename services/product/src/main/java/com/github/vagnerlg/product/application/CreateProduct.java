package com.github.vagnerlg.product.application;

import java.math.BigDecimal;

public record CreateProduct(String name, String description, BigDecimal price, String category) {
}
