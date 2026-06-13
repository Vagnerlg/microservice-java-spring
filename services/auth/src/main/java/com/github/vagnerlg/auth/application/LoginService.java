package com.github.vagnerlg.auth.application;

import com.github.vagnerlg.auth.domain.AuthToken;
import com.github.vagnerlg.auth.infrastructure.keycloak.KeycloakClient;
import org.springframework.stereotype.Service;

@Service
public class LoginService {

    private final KeycloakClient keycloakClient;

    public LoginService(KeycloakClient keycloakClient) {
        this.keycloakClient = keycloakClient;
    }

    public AuthToken login(String username, String password) {
        return keycloakClient.getToken(username, password);
    }
}
