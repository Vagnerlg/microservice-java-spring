package com.github.vagnerlg.user.application;

import com.github.vagnerlg.user.domain.User;
import com.github.vagnerlg.user.domain.UserRepository;
import com.github.vagnerlg.user.domain.exception.UserNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void create_shouldPersistUser_whenNotExists() {
        when(userRepository.existsByKeycloakId("kc-123")).thenReturn(false);
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        userService.create("kc-123", "john", "John Doe", Instant.now());

        verify(userRepository).save(any(User.class));
    }

    @Test
    void create_shouldIgnore_whenUserAlreadyExists() {
        when(userRepository.existsByKeycloakId("kc-123")).thenReturn(true);

        userService.create("kc-123", "john", "John Doe", Instant.now());

        verify(userRepository, never()).save(any());
    }

    @Test
    void findByKeycloakId_shouldReturnUser_whenExists() {
        var user = new User(UUID.randomUUID(), "kc-123", "john", "John Doe", Instant.now());
        when(userRepository.findByKeycloakId("kc-123")).thenReturn(Optional.of(user));

        var result = userService.findByKeycloakId("kc-123");

        assertThat(result.keycloakId()).isEqualTo("kc-123");
        assertThat(result.username()).isEqualTo("john");
    }

    @Test
    void findByKeycloakId_shouldThrow_whenNotFound() {
        when(userRepository.findByKeycloakId("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findByKeycloakId("unknown"))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("unknown");
    }
}
