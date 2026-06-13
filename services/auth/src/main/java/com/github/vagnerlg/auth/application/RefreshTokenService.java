package com.github.vagnerlg.auth.application;

import com.github.vagnerlg.auth.domain.AuthToken;
import com.github.vagnerlg.auth.infrastructure.keycloak.KeycloakClient;
import org.springframework.stereotype.Service;

@Service
public class RefreshTokenService {

    private final KeycloakClient keycloakClient;

    public RefreshTokenService(KeycloakClient keycloakClient) {
        this.keycloakClient = keycloakClient;
    }

    public AuthToken refresh(String refreshToken) {
        return keycloakClient.refreshToken(refreshToken);
    }
}
