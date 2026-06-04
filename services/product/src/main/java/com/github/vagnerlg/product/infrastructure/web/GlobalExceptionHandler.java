package com.github.vagnerlg.product.infrastructure.web;

import com.github.vagnerlg.product.domain.exception.ProductAlreadyExistsException;
import com.github.vagnerlg.product.domain.exception.ProductNotFoundException;
import com.github.vagnerlg.product.infrastructure.web.response.ApiError;
import com.github.vagnerlg.product.infrastructure.web.response.ApiErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    ApiErrorResponse handleValidation(MethodArgumentNotValidException ex) {
        List<ApiError> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ApiError(fe.getField(), fe.getDefaultMessage()))
                .toList();
        return ApiErrorResponse.of(errors);
    }

    @ExceptionHandler(ProductNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    ApiErrorResponse handleNotFound(ProductNotFoundException ex) {
        return ApiErrorResponse.single(ex.getMessage());
    }

    @ExceptionHandler(ProductAlreadyExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    ApiErrorResponse handleConflict(ProductAlreadyExistsException ex) {
        return ApiErrorResponse.single(ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    ApiErrorResponse handleGeneric(Exception ex) {
        log.error("Unexpected error", ex);
        return ApiErrorResponse.single("An unexpected error occurred");
    }
}
