package com.github.vagnerlg.auth.application;

import com.github.vagnerlg.auth.domain.AuthToken;
import com.github.vagnerlg.auth.domain.exception.InvalidCredentialsException;
import com.github.vagnerlg.auth.infrastructure.keycloak.KeycloakClient;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

class RefreshTokenServiceTest {

    private final KeycloakClient keycloakClient = Mockito.mock(KeycloakClient.class);
    private final RefreshTokenService service = new RefreshTokenService(keycloakClient);

    @Test
    void refresh_returnsNewTokens() {
        var expected = new AuthToken("new-access", "new-refresh", 300L);
        given(keycloakClient.refreshToken("old-refresh")).willReturn(expected);

        var result = service.refresh("old-refresh");

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void refresh_propagatesInvalidCredentialsWhenTokenExpired() {
        given(keycloakClient.refreshToken(any())).willThrow(new InvalidCredentialsException());

        assertThatThrownBy(() -> service.refresh("expired-token"))
                .isInstanceOf(InvalidCredentialsException.class);
    }
}
