package com.github.vagnerlg.user.infrastructure.web;

import com.github.vagnerlg.user.MockConfiguration;
import com.github.vagnerlg.user.TestcontainersConfiguration;
import com.github.vagnerlg.user.application.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({TestcontainersConfiguration.class, MockConfiguration.class})
class UserControllerIT {

    @LocalServerPort
    private int port;

    @Autowired
    private UserService userService;

    private RestClient restClient() {
        return RestClient.builder()
                .baseUrl("http://localhost:" + port)
                .defaultStatusHandler(status -> true, (request, response) -> {})
                .build();
    }

    @Test
    void me_shouldReturn200_withUserProfile() {
        userService.create("user-found", "john", "John Doe", Instant.now());

        ResponseEntity<Map> response = restClient().get()
                .uri("/users/me")
                .header("Authorization", "Bearer user-found")
                .retrieve()
                .toEntity(Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
        assertThat(data.get("username")).isEqualTo("john");
        assertThat(data.get("name")).isEqualTo("John Doe");
    }

    @Test
    void me_shouldReturn404_whenUserNotFound() {
        ResponseEntity<Map> response = restClient().get()
                .uri("/users/me")
                .header("Authorization", "Bearer nonexistent-user")
                .retrieve()
                .toEntity(Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).containsKey("errors");
    }

    @Test
    void me_shouldReturn401_whenNoToken() {
        ResponseEntity<Map> response = restClient().get()
                .uri("/users/me")
                .retrieve()
                .toEntity(Map.class);

        assertThat(response.getStatusCode().value()).isEqualTo(401);
    }
}
