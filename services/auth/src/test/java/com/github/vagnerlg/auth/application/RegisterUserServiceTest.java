package com.github.vagnerlg.auth.application;

import com.github.vagnerlg.auth.domain.exception.UserAlreadyExistsException;
import com.github.vagnerlg.auth.infrastructure.kafka.KafkaUserEventPublisher;
import com.github.vagnerlg.auth.infrastructure.keycloak.KeycloakClient;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class RegisterUserServiceTest {

    private final KeycloakClient keycloakClient = Mockito.mock(KeycloakClient.class);
    private final KafkaUserEventPublisher eventPublisher = Mockito.mock(KafkaUserEventPublisher.class);
    private final RegisterUserService service = new RegisterUserService(keycloakClient, eventPublisher);

    @Test
    void register_createsUserAndPublishesEvent() {
        given(keycloakClient.createUser("vagner", "Vagner Silva", "pass1234")).willReturn("kc-id-1");

        var user = service.register("vagner", "Vagner Silva", "pass1234");

        assertThat(user.keycloakId()).isEqualTo("kc-id-1");
        assertThat(user.username()).isEqualTo("vagner");
        assertThat(user.name()).isEqualTo("Vagner Silva");
        assertThat(user.createdAt()).isNotNull();
        verify(eventPublisher).publish(user);
    }

    @Test
    void register_propagatesUserAlreadyExistsException() {
        given(keycloakClient.createUser(any(), any(), any()))
                .willThrow(new UserAlreadyExistsException("vagner"));

        assertThatThrownBy(() -> service.register("vagner", "Vagner Silva", "pass1234"))
                .isInstanceOf(UserAlreadyExistsException.class);

        verifyNoInteractions(eventPublisher);
    }
}
