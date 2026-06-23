package com.github.vagnerlg.cart.infrastructure.web.response;

import com.github.vagnerlg.cart.domain.CartItem;

import java.math.BigDecimal;

public record CartItemResponse(String productId, String name, BigDecimal price, int quantity) {

    public static CartItemResponse from(CartItem item) {
        return new CartItemResponse(item.productId(), item.name(), item.price(), item.quantity());
    }
}
