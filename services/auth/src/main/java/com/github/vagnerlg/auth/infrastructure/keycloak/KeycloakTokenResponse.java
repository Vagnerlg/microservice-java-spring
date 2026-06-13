package com.github.vagnerlg.auth.infrastructure.keycloak;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
record KeycloakTokenResponse(String accessToken, String refreshToken, long expiresIn) {
}
