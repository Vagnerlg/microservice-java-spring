package com.github.vagnerlg.auth.infrastructure.web;

import com.github.vagnerlg.auth.TestcontainersConfiguration;
import com.github.vagnerlg.auth.infrastructure.redis.RedisTokenBlacklist;
import com.github.vagnerlg.auth.infrastructure.web.request.LoginRequest;
import com.github.vagnerlg.auth.infrastructure.web.request.LogoutRequest;
import com.github.vagnerlg.auth.infrastructure.web.request.RefreshRequest;
import com.github.vagnerlg.auth.infrastructure.web.request.RegisterRequest;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AuthControllerIT {

    @LocalServerPort
    int port;

    @Autowired
    RedisTokenBlacklist blacklist;

    @Value("${spring.kafka.bootstrap-servers}")
    String kafkaBootstrapServers;

    RestClient restClient;

    static final String TEST_USERNAME = "ituser";
    static final String TEST_PASSWORD = "Password123!";

    @BeforeAll
    void setUp() {
        restClient = RestClient.builder()
                .baseUrl("http://localhost:" + port)
                .defaultStatusHandler(status -> true, (request, response) -> {})
                .build();

        ResponseEntity<String> reg = restClient.post()
                .uri("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new RegisterRequest(TEST_USERNAME, "IT User", TEST_PASSWORD))
                .retrieve()
                .toEntity(String.class);

        assertThat(reg.getStatusCode().value())
                .as("@BeforeAll register ituser: %s", reg.getBody())
                .isEqualTo(201);
    }

    @Test
    void register_returns201AndPublishesKafkaEvent() {
        var props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "it-" + UUID.randomUUID());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        try (var consumer = new KafkaConsumer<String, String>(props)) {
            consumer.subscribe(List.of("user"));
            consumer.poll(Duration.ofMillis(500));

            ResponseEntity<String> response = restClient.post()
                    .uri("/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new RegisterRequest("newuser", "New User", "Password123!"))
                    .retrieve()
                    .toEntity(String.class);

            assertThat(response.getStatusCode().value()).isEqualTo(201);
            assertThat(response.getBody()).contains("keycloakId").contains("newuser");

            var records = KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(10));
            assertThat(records.count()).isGreaterThanOrEqualTo(1);
            assertThat(records.iterator().next().value()).contains("\"event\":\"CREATED\"");
        }
    }

    @Test
    void register_returns409WhenUsernameAlreadyExists() {
        var request = new RegisterRequest("duplicateuser", "Duplicate User", "Password123!");
        restClient.post().uri("/auth/register").contentType(MediaType.APPLICATION_JSON)
                .body(request).retrieve().toBodilessEntity();

        ResponseEntity<String> response = restClient.post()
                .uri("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toEntity(String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(409);
    }

    @Test
    void register_returns422WhenRequestIsInvalid() {
        var request = new RegisterRequest("", "Name", "short");

        ResponseEntity<String> response = restClient.post()
                .uri("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toEntity(String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(422);
    }

    @Test
    void login_returns200WithTokens() {
        ResponseEntity<String> response = restClient.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new LoginRequest(TEST_USERNAME, TEST_PASSWORD))
                .retrieve()
                .toEntity(String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).contains("accessToken").contains("refreshToken");
    }

    @Test
    void login_returns401WithWrongPassword() {
        ResponseEntity<String> response = restClient.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new LoginRequest(TEST_USERNAME, "wrongpassword"))
                .retrieve()
                .toEntity(String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(401);
    }

    @Test
    void refresh_returns200WithNewTokens() {
        String loginBody = loginAs(TEST_USERNAME, TEST_PASSWORD);
        String refreshToken = extractField(loginBody, "refreshToken");

        ResponseEntity<String> response = restClient.post()
                .uri("/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new RefreshRequest(refreshToken))
                .retrieve()
                .toEntity(String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).contains("accessToken");
    }

    @Test
    void refresh_returns401WhenRefreshTokenIsInvalid() {
        ResponseEntity<String> response = restClient.post()
                .uri("/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new RefreshRequest("invalid.garbage.refresh.token"))
                .retrieve()
                .toEntity(String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(401);
    }

    @Test
    void logout_returns204AndBlacklistsJti() {
        String loginBody = loginAs(TEST_USERNAME, TEST_PASSWORD);
        String accessToken = extractField(loginBody, "accessToken");
        String refreshToken = extractField(loginBody, "refreshToken");

        ResponseEntity<Void> response = restClient.post()
                .uri("/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new LogoutRequest(accessToken, refreshToken))
                .retrieve()
                .toEntity(Void.class);

        assertThat(response.getStatusCode().value()).isEqualTo(204);

        String jti = extractJtiFromJwt(accessToken);
        assertThat(blacklist.isBlacklisted(jti)).isTrue();
    }

    private String loginAs(String username, String password) {
        return restClient.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new LoginRequest(username, password))
                .retrieve()
                .body(String.class);
    }

    private String extractField(String json, String field) {
        String key = "\"" + field + "\":\"";
        int start = json.indexOf(key) + key.length();
        int end = json.indexOf('"', start);
        return json.substring(start, end);
    }

    private String extractJtiFromJwt(String token) {
        String payload = new String(Base64.getUrlDecoder().decode(token.split("\\.")[1]));
        String key = "\"jti\":\"";
        int start = payload.indexOf(key) + key.length();
        int end = payload.indexOf('"', start);
        return payload.substring(start, end);
    }
}
