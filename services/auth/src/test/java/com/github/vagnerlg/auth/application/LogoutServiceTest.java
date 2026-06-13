package com.github.vagnerlg.auth.application;

import tools.jackson.databind.ObjectMapper;
import com.github.vagnerlg.auth.infrastructure.keycloak.KeycloakClient;
import com.github.vagnerlg.auth.infrastructure.redis.RedisTokenBlacklist;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.Base64;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.longThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class LogoutServiceTest {

    private final KeycloakClient keycloakClient = Mockito.mock(KeycloakClient.class);
    private final RedisTokenBlacklist blacklist = Mockito.mock(RedisTokenBlacklist.class);
    private final LogoutService service = new LogoutService(keycloakClient, blacklist, new ObjectMapper());

    @Test
    void logout_revokesRefreshTokenAndBlacklistsJti() {
        String token = buildFakeJwt("test-jti-1", Instant.now().plusSeconds(300).getEpochSecond());

        service.logout(token, "refresh-token");

        verify(keycloakClient).logout("refresh-token");
        verify(blacklist).add(eq("test-jti-1"), longThat(ttl -> ttl > 0 && ttl <= 300));
    }

    @Test
    void logout_skipsBlacklistWhenTokenAlreadyExpired() {
        String token = buildFakeJwt("expired-jti", Instant.now().minusSeconds(10).getEpochSecond());

        service.logout(token, "refresh-token");

        verify(keycloakClient).logout("refresh-token");
        verifyNoInteractions(blacklist);
    }

    @Test
    void logout_skipsBlacklistWhenTokenIsMalformed() {
        service.logout("not.a.jwt", "refresh-token");

        verify(keycloakClient).logout("refresh-token");
        verifyNoInteractions(blacklist);
    }

    private String buildFakeJwt(String jti, long exp) {
        String header = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"alg\":\"RS256\",\"typ\":\"JWT\"}".getBytes());
        String payload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(("{\"jti\":\"" + jti + "\",\"exp\":" + exp + "}").getBytes());
        return header + "." + payload + ".signature";
    }
}
