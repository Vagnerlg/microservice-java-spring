package com.github.vagnerlg.cart.domain;

public interface CartRepository {

    Cart findByUserId(String userId);

    Cart save(Cart cart);

    void deleteByUserId(String userId);
}
