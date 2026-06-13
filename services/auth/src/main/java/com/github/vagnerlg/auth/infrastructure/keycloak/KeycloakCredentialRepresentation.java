package com.github.vagnerlg.auth.infrastructure.keycloak;

record KeycloakCredentialRepresentation(String type, String value, boolean temporary) {
}
