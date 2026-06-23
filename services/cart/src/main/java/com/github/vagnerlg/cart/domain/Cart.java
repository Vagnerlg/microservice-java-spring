package com.github.vagnerlg.cart.domain;

import com.github.vagnerlg.cart.domain.exception.CartItemNotFoundException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public record Cart(String userId, List<CartItem> items, Instant updatedAt) {

    public Cart {
        items = List.copyOf(items);
    }

    public static Cart empty(String userId) {
        return new Cart(userId, List.of(), Instant.now());
    }

    public Cart withItem(CartItem newItem) {
        var existing = items.stream()
                .filter(i -> i.productId().equals(newItem.productId()))
                .findFirst();

        List<CartItem> updated;
        if (existing.isPresent()) {
            var merged = new CartItem(
                    existing.get().productId(),
                    existing.get().name(),
                    existing.get().price(),
                    existing.get().quantity() + newItem.quantity());
            updated = items.stream()
                    .map(i -> i.productId().equals(newItem.productId()) ? merged : i)
                    .toList();
        } else {
            updated = new ArrayList<>(items);
            updated.add(newItem);
        }

        return new Cart(userId, updated, Instant.now());
    }

    public Cart withUpdatedItem(String productId, int quantity) {
        if (items.stream().noneMatch(i -> i.productId().equals(productId))) {
            throw new CartItemNotFoundException(productId);
        }
        var updated = items.stream()
                .map(i -> i.productId().equals(productId)
                        ? new CartItem(i.productId(), i.name(), i.price(), quantity)
                        : i)
                .toList();
        return new Cart(userId, updated, Instant.now());
    }

    public Cart withoutItem(String productId) {
        var updated = items.stream()
                .filter(i -> !i.productId().equals(productId))
                .toList();
        return new Cart(userId, updated, Instant.now());
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }
}
