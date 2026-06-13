package com.github.vagnerlg.auth.domain;

public record AuthToken(String accessToken, String refreshToken, long expiresIn) {
}
