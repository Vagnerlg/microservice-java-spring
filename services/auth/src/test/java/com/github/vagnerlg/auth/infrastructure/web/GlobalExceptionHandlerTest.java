package com.github.vagnerlg.auth.infrastructure.web;

import com.github.vagnerlg.auth.infrastructure.web.response.ApiErrorResponse;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleGeneric_returnsInternalErrorResponse() {
        ApiErrorResponse response = handler.handleGeneric(new RuntimeException("Unexpected DB failure"));

        assertThat(response.errors()).hasSize(1);
        assertThat(response.errors().getFirst().message()).isEqualTo("An unexpected error occurred");
    }
}
