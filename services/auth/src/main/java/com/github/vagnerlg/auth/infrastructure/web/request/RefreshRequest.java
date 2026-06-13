package com.github.vagnerlg.auth.infrastructure.web.request;

import jakarta.validation.constraints.NotBlank;

public record RefreshRequest(@NotBlank String refreshToken) {
}
