package com.github.vagnerlg.cart.infrastructure.web.request;

import jakarta.validation.constraints.Positive;

public record UpdateItemRequest(@Positive int quantity) {
}
