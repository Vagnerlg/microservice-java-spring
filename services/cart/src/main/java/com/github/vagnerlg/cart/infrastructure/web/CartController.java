package com.github.vagnerlg.cart.infrastructure.web;

import com.github.vagnerlg.cart.application.CartService;
import com.github.vagnerlg.cart.domain.CartItem;
import com.github.vagnerlg.cart.infrastructure.web.request.AddItemRequest;
import com.github.vagnerlg.cart.infrastructure.web.request.UpdateItemRequest;
import com.github.vagnerlg.cart.infrastructure.web.response.ApiResponse;
import com.github.vagnerlg.cart.infrastructure.web.response.CartResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/carts")
class CartController {

    private final CartService cartService;

    CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    ApiResponse<CartResponse> getCart(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.of(CartResponse.from(cartService.getCart(jwt.getSubject())));
    }

    @PostMapping("/items")
    ApiResponse<CartResponse> addItem(@AuthenticationPrincipal Jwt jwt,
                                      @Valid @RequestBody AddItemRequest request) {
        var item = new CartItem(request.productId(), request.name(), request.price(), request.quantity());
        return ApiResponse.of(CartResponse.from(cartService.addItem(jwt.getSubject(), item)));
    }

    @PutMapping("/items/{productId}")
    ApiResponse<CartResponse> updateItem(@AuthenticationPrincipal Jwt jwt,
                                         @PathVariable String productId,
                                         @Valid @RequestBody UpdateItemRequest request) {
        return ApiResponse.of(CartResponse.from(cartService.updateItem(jwt.getSubject(), productId, request.quantity())));
    }

    @DeleteMapping("/items/{productId}")
    ApiResponse<CartResponse> removeItem(@AuthenticationPrincipal Jwt jwt,
                                         @PathVariable String productId) {
        return ApiResponse.of(CartResponse.from(cartService.removeItem(jwt.getSubject(), productId)));
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void clearCart(@AuthenticationPrincipal Jwt jwt) {
        cartService.clearCart(jwt.getSubject());
    }

    @PostMapping("/checkout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void checkout(@AuthenticationPrincipal Jwt jwt) {
        cartService.checkout(jwt.getSubject());
    }
}
