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

class LoginServiceTest {

    private final KeycloakClient keycloakClient = Mockito.mock(KeycloakClient.class);
    private final LoginService service = new LoginService(keycloakClient);

    @Test
    void login_returnsToken() {
        var expected = new AuthToken("access-token", "refresh-token", 300L);
        given(keycloakClient.getToken("vagner", "pass1234")).willReturn(expected);

        var result = service.login("vagner", "pass1234");

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void login_propagatesInvalidCredentialsException() {
        given(keycloakClient.getToken(any(), any())).willThrow(new InvalidCredentialsException());

        assertThatThrownBy(() -> service.login("vagner", "wrong"))
                .isInstanceOf(InvalidCredentialsException.class);
    }
}
