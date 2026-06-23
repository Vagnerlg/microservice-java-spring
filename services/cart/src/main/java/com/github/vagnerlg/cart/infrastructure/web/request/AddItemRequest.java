package com.github.vagnerlg.cart.infrastructure.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record AddItemRequest(
        @NotBlank String productId,
        @NotBlank String name,
        @NotNull @Positive BigDecimal price,
        @Positive int quantity
) {
}
