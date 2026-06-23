package com.github.vagnerlg.order.infrastructure.web;

import com.github.vagnerlg.order.domain.exception.OrderAccessDeniedException;
import com.github.vagnerlg.order.domain.exception.OrderCancellationException;
import com.github.vagnerlg.order.domain.exception.OrderNotFoundException;
import com.github.vagnerlg.order.infrastructure.web.response.ApiErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(OrderNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    ApiErrorResponse handleNotFound(OrderNotFoundException ex) {
        return ApiErrorResponse.single(ex.getMessage());
    }

    @ExceptionHandler(OrderAccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    ApiErrorResponse handleAccessDenied(OrderAccessDeniedException ex) {
        return ApiErrorResponse.single(ex.getMessage());
    }

    @ExceptionHandler(OrderCancellationException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    ApiErrorResponse handleCancellation(OrderCancellationException ex) {
        return ApiErrorResponse.single(ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    ApiErrorResponse handleGeneric(Exception ex) {
        log.error("Unexpected error", ex);
        return ApiErrorResponse.single("An unexpected error occurred");
    }
}
