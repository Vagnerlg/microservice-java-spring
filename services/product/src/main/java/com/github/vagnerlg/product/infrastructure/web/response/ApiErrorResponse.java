package com.github.vagnerlg.product.infrastructure.web.response;

import java.util.List;

public record ApiErrorResponse(List<ApiError> errors) {

    public static ApiErrorResponse of(List<ApiError> errors) {
        return new ApiErrorResponse(errors);
    }

    public static ApiErrorResponse single(String message) {
        return new ApiErrorResponse(List.of(new ApiError(null, message)));
    }
}
