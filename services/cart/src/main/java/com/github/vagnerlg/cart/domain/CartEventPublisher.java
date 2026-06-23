package com.github.vagnerlg.cart.domain;

public interface CartEventPublisher {

    void publishCheckout(Cart cart);
}
