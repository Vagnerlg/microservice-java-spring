package com.github.vagnerlg.cart.infrastructure.web.response;

import java.util.List;

public record ApiErrorResponse(List<ApiError> errors) {

    public static ApiErrorResponse of(String field, String message) {
        return new ApiErrorResponse(List.of(new ApiError(field, message)));
    }
}
