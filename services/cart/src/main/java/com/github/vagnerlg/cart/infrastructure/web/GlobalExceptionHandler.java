package com.github.vagnerlg.cart.infrastructure.web;

import com.github.vagnerlg.cart.domain.exception.CartEmptyException;
import com.github.vagnerlg.cart.domain.exception.CartItemNotFoundException;
import com.github.vagnerlg.cart.infrastructure.web.response.ApiError;
import com.github.vagnerlg.cart.infrastructure.web.response.ApiErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(CartItemNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    ApiErrorResponse handleCartItemNotFound(CartItemNotFoundException ex) {
        return ApiErrorResponse.of("productId", ex.getMessage());
    }

    @ExceptionHandler(CartEmptyException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    ApiErrorResponse handleCartEmpty(CartEmptyException ex) {
        return ApiErrorResponse.of("cart", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    ApiErrorResponse handleValidation(MethodArgumentNotValidException ex) {
        List<ApiError> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getField)
                .distinct()
                .map(field -> new ApiError(field, "must not be blank or must be positive"))
                .toList();
        return new ApiErrorResponse(errors);
    }
}
