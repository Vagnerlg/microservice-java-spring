package com.github.vagnerlg.cart.domain;

import com.github.vagnerlg.cart.domain.exception.CartItemNotFoundException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CartTest {

    private static final CartItem ITEM_A = new CartItem("prod-a", "Item A", BigDecimal.TEN, 1);
    private static final CartItem ITEM_B = new CartItem("prod-b", "Item B", BigDecimal.ONE, 2);

    @Test
    void empty_createsCartWithNoItems() {
        var cart = Cart.empty("user-1");
        assertThat(cart.userId()).isEqualTo("user-1");
        assertThat(cart.items()).isEmpty();
        assertThat(cart.isEmpty()).isTrue();
    }

    @Test
    void withItem_addsNewItem() {
        var cart = Cart.empty("user-1").withItem(ITEM_A);
        assertThat(cart.items()).hasSize(1);
        assertThat(cart.isEmpty()).isFalse();
    }

    @Test
    void withItem_mergesQuantityForExistingProduct_andKeepsOtherItems() {
        // cart with two items — ensures both ternary branches in the merge map are covered
        var cart = Cart.empty("user-1").withItem(ITEM_A).withItem(ITEM_B);
        var extra = new CartItem("prod-a", "Item A", BigDecimal.TEN, 3);

        var result = cart.withItem(extra);

        assertThat(result.items()).hasSize(2);
        var updatedA = result.items().stream().filter(i -> i.productId().equals("prod-a")).findFirst().orElseThrow();
        var keptB = result.items().stream().filter(i -> i.productId().equals("prod-b")).findFirst().orElseThrow();
        assertThat(updatedA.quantity()).isEqualTo(4);
        assertThat(keptB.quantity()).isEqualTo(2);
    }

    @Test
    void withUpdatedItem_updatesQuantity_andKeepsOtherItems() {
        // two items — ensures both ternary branches in the update map are covered
        var cart = Cart.empty("user-1").withItem(ITEM_A).withItem(ITEM_B);

        var result = cart.withUpdatedItem("prod-a", 10);

        assertThat(result.items()).hasSize(2);
        var updatedA = result.items().stream().filter(i -> i.productId().equals("prod-a")).findFirst().orElseThrow();
        var keptB = result.items().stream().filter(i -> i.productId().equals("prod-b")).findFirst().orElseThrow();
        assertThat(updatedA.quantity()).isEqualTo(10);
        assertThat(keptB.quantity()).isEqualTo(2);
    }

    @Test
    void withUpdatedItem_throwsWhenProductNotFound() {
        var cart = Cart.empty("user-1").withItem(ITEM_A);
        assertThatThrownBy(() -> cart.withUpdatedItem("non-existent", 5))
                .isInstanceOf(CartItemNotFoundException.class);
    }

    @Test
    void withoutItem_removesMatchingItem_andKeepsOther() {
        var cart = Cart.empty("user-1").withItem(ITEM_A).withItem(ITEM_B);

        var result = cart.withoutItem("prod-a");

        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).productId()).isEqualTo("prod-b");
    }

    @Test
    void withoutItem_nonExistentProduct_returnsUnchangedCart() {
        var cart = Cart.empty("user-1").withItem(ITEM_A);
        var result = cart.withoutItem("non-existent");
        assertThat(result.items()).hasSize(1);
    }
}
