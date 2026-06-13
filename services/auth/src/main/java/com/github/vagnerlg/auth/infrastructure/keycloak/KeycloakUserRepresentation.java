package com.github.vagnerlg.auth.infrastructure.keycloak;

import java.util.List;

record KeycloakUserRepresentation(
        String username,
        String firstName,
        String email,
        boolean emailVerified,
        boolean enabled,
        List<String> requiredActions
) {
}
