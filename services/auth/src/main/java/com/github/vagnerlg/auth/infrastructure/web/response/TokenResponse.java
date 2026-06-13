package com.github.vagnerlg.auth.infrastructure.web.response;

public record TokenResponse(String accessToken, String refreshToken, long expiresIn) {
}
