package com.github.vagnerlg.auth.infrastructure.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank String username,
        @NotBlank String name,
        @NotBlank @Size(min = 8) String password
) {
}
