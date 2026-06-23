package com.github.vagnerlg.cart.infrastructure.web.response;

import com.github.vagnerlg.cart.domain.Cart;

import java.time.Instant;
import java.util.List;

public record CartResponse(String userId, List<CartItemResponse> items, Instant updatedAt) {

    public static CartResponse from(Cart cart) {
        var items = cart.items().stream().map(CartItemResponse::from).toList();
        return new CartResponse(cart.userId(), items, cart.updatedAt());
    }
}
