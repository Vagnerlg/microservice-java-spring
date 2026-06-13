package com.github.vagnerlg.user.infrastructure.web;

import com.github.vagnerlg.user.domain.exception.UserNotFoundException;
import com.github.vagnerlg.user.infrastructure.web.response.ApiErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(UserNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    ApiErrorResponse handleNotFound(UserNotFoundException ex) {
        return ApiErrorResponse.single(ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    ApiErrorResponse handleGeneric(Exception ex) {
        log.error("Unexpected error", ex);
        return ApiErrorResponse.single("An unexpected error occurred");
    }
}
