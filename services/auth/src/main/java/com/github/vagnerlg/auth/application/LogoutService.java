package com.github.vagnerlg.auth.application;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import com.github.vagnerlg.auth.infrastructure.keycloak.KeycloakClient;
import com.github.vagnerlg.auth.infrastructure.redis.RedisTokenBlacklist;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Base64;
import java.util.Collections;
import java.util.Map;

@Service
public class LogoutService {

    private final KeycloakClient keycloakClient;
    private final RedisTokenBlacklist blacklist;
    private final ObjectMapper objectMapper;

    public LogoutService(KeycloakClient keycloakClient, RedisTokenBlacklist blacklist, ObjectMapper objectMapper) {
        this.keycloakClient = keycloakClient;
        this.blacklist = blacklist;
        this.objectMapper = objectMapper;
    }

    public void logout(String accessToken, String refreshToken) {
        Map<String, Object> claims = decodeJwtPayload(accessToken);
        String jti = (String) claims.get("jti");
        Object expClaim = claims.get("exp");
        long exp = expClaim != null ? ((Number) expClaim).longValue() : 0L;
        long ttl = exp - Instant.now().getEpochSecond();

        keycloakClient.logout(refreshToken);

        if (jti != null && ttl > 0) {
            blacklist.add(jti, ttl);
        }
    }

    private Map<String, Object> decodeJwtPayload(String token) {
        try {
            String[] parts = token.split("\\.");
            byte[] payload = Base64.getUrlDecoder().decode(parts[1]);
            return objectMapper.readValue(payload, new TypeReference<Map<String, Object>>() {});
        } catch (RuntimeException e) {
            return Collections.emptyMap();
        }
    }
}
