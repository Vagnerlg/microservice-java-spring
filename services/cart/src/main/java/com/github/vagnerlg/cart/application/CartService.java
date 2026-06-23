package com.github.vagnerlg.cart.application;

import com.github.vagnerlg.cart.domain.Cart;
import com.github.vagnerlg.cart.domain.CartEventPublisher;
import com.github.vagnerlg.cart.domain.CartItem;
import com.github.vagnerlg.cart.domain.CartRepository;
import com.github.vagnerlg.cart.domain.exception.CartEmptyException;
import org.springframework.stereotype.Service;

@Service
public class CartService {

    private final CartRepository cartRepository;
    private final CartEventPublisher cartEventPublisher;

    CartService(CartRepository cartRepository, CartEventPublisher cartEventPublisher) {
        this.cartRepository = cartRepository;
        this.cartEventPublisher = cartEventPublisher;
    }

    public Cart getCart(String userId) {
        return cartRepository.findByUserId(userId);
    }

    public Cart addItem(String userId, CartItem item) {
        var cart = cartRepository.findByUserId(userId);
        return cartRepository.save(cart.withItem(item));
    }

    public Cart updateItem(String userId, String productId, int quantity) {
        var cart = cartRepository.findByUserId(userId);
        return cartRepository.save(cart.withUpdatedItem(productId, quantity));
    }

    public Cart removeItem(String userId, String productId) {
        var cart = cartRepository.findByUserId(userId);
        return cartRepository.save(cart.withoutItem(productId));
    }

    public void clearCart(String userId) {
        cartRepository.deleteByUserId(userId);
    }

    public void checkout(String userId) {
        var cart = cartRepository.findByUserId(userId);
        if (cart.isEmpty()) {
            throw new CartEmptyException(userId);
        }
        cartEventPublisher.publishCheckout(cart);
        cartRepository.deleteByUserId(userId);
    }
}
