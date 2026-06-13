package com.github.vagnerlg.auth.infrastructure.web.request;

import jakarta.validation.constraints.NotBlank;

public record LogoutRequest(
        @NotBlank String accessToken,
        @NotBlank String refreshToken
) {
}
