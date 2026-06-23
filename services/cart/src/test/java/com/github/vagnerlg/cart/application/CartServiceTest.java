package com.github.vagnerlg.cart.application;

import com.github.vagnerlg.cart.domain.Cart;
import com.github.vagnerlg.cart.domain.CartEventPublisher;
import com.github.vagnerlg.cart.domain.CartItem;
import com.github.vagnerlg.cart.domain.CartRepository;
import com.github.vagnerlg.cart.domain.exception.CartEmptyException;
import com.github.vagnerlg.cart.domain.exception.CartItemNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CartServiceTest {

    private CartRepository cartRepository;
    private CartEventPublisher cartEventPublisher;
    private CartService cartService;

    @BeforeEach
    void setUp() {
        cartRepository = mock(CartRepository.class);
        cartEventPublisher = mock(CartEventPublisher.class);
        cartService = new CartService(cartRepository, cartEventPublisher);
    }

    @Test
    void getCart_returnsCartFromRepository() {
        var cart = Cart.empty("user-1");
        when(cartRepository.findByUserId("user-1")).thenReturn(cart);

        var result = cartService.getCart("user-1");

        assertThat(result.userId()).isEqualTo("user-1");
        assertThat(result.items()).isEmpty();
    }

    @Test
    void addItem_withNewProduct_addsItemToCart() {
        var empty = Cart.empty("user-1");
        var item = new CartItem("prod-1", "Tênis X", BigDecimal.valueOf(299.90), 2);
        when(cartRepository.findByUserId("user-1")).thenReturn(empty);
        when(cartRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = cartService.addItem("user-1", item);

        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).quantity()).isEqualTo(2);
    }

    @Test
    void addItem_withExistingProduct_sumsQuantity() {
        var existing = new CartItem("prod-1", "Tênis X", BigDecimal.valueOf(299.90), 2);
        var cart = Cart.empty("user-1").withItem(existing);
        when(cartRepository.findByUserId("user-1")).thenReturn(cart);
        when(cartRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = cartService.addItem("user-1", new CartItem("prod-1", "Tênis X", BigDecimal.valueOf(299.90), 3));

        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).quantity()).isEqualTo(5);
    }

    @Test
    void updateItem_withValidProduct_updatesQuantity() {
        var item = new CartItem("prod-1", "Tênis X", BigDecimal.valueOf(299.90), 2);
        var cart = Cart.empty("user-1").withItem(item);
        when(cartRepository.findByUserId("user-1")).thenReturn(cart);
        when(cartRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = cartService.updateItem("user-1", "prod-1", 5);

        assertThat(result.items().get(0).quantity()).isEqualTo(5);
    }

    @Test
    void updateItem_withNonExistentProduct_throwsException() {
        when(cartRepository.findByUserId("user-1")).thenReturn(Cart.empty("user-1"));

        assertThatThrownBy(() -> cartService.updateItem("user-1", "prod-999", 5))
                .isInstanceOf(CartItemNotFoundException.class);
    }

    @Test
    void removeItem_removesItemFromCart() {
        var item = new CartItem("prod-1", "Tênis X", BigDecimal.valueOf(299.90), 2);
        var cart = Cart.empty("user-1").withItem(item);
        when(cartRepository.findByUserId("user-1")).thenReturn(cart);
        when(cartRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = cartService.removeItem("user-1", "prod-1");

        assertThat(result.items()).isEmpty();
    }

    @Test
    void clearCart_deletesCart() {
        cartService.clearCart("user-1");

        verify(cartRepository).deleteByUserId("user-1");
    }

    @Test
    void checkout_withNonEmptyCart_publishesEventAndDeletesCart() {
        var item = new CartItem("prod-1", "Tênis X", BigDecimal.valueOf(299.90), 2);
        var cart = Cart.empty("user-1").withItem(item);
        when(cartRepository.findByUserId("user-1")).thenReturn(cart);

        cartService.checkout("user-1");

        verify(cartEventPublisher).publishCheckout(cart);
        verify(cartRepository).deleteByUserId("user-1");
    }

    @Test
    void checkout_withEmptyCart_throwsException() {
        when(cartRepository.findByUserId("user-1")).thenReturn(Cart.empty("user-1"));

        assertThatThrownBy(() -> cartService.checkout("user-1"))
                .isInstanceOf(CartEmptyException.class);
        verify(cartEventPublisher, never()).publishCheckout(any());
    }
}
