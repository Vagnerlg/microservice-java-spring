package com.github.vagnerlg.cart.domain;

import java.math.BigDecimal;

public record CartItem(String productId, String name, BigDecimal price, int quantity) {
}
