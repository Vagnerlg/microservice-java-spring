package com.github.vagnerlg.product.infrastructure.web;

import com.github.vagnerlg.product.TestcontainersConfiguration;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class ProductControllerIT {

    @LocalServerPort
    private int port;

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    private RestClient restClient;

    @BeforeEach
    void setup() {
        restClient = RestClient.builder()
                .baseUrl("http://localhost:" + port)
                .defaultStatusHandler(status -> true, (request, response) -> {})
                .build();
    }

    @Test
    void createProduct_shouldReturn201_withDataEnvelope() {
        var body = """
                {"name":"Notebook Pro","description":"High performance laptop","price":4999.99,"category":"Electronics"}
                """;

        ResponseEntity<Map> response = restClient.post()
                .uri("/products")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toEntity(Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
        assertThat(data.get("id")).isNotNull();
        assertThat(data.get("name")).isEqualTo("Notebook Pro");
    }

    @Test
    void createProduct_shouldReturn422_whenValidationFails() {
        var body = """
                {"name":"","description":"desc","price":-1,"category":"Electronics"}
                """;

        ResponseEntity<Map> response = restClient.post()
                .uri("/products")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toEntity(Map.class);

        assertThat(response.getStatusCode().value()).isEqualTo(422);
        assertThat(response.getBody()).containsKey("errors");
    }

    @Test
    void createProduct_shouldReturn409_whenNameAlreadyExists() {
        var body = """
                {"name":"Duplicate Product","description":"desc","price":10.00,"category":"General"}
                """;

        restClient.post().uri("/products").contentType(MediaType.APPLICATION_JSON).body(body)
                .retrieve().toBodilessEntity();

        ResponseEntity<Map> response = restClient.post()
                .uri("/products")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toEntity(Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).containsKey("errors");
    }

    @Test
    void findById_shouldReturn200_whenProductExists() {
        var body = """
                {"name":"Mouse Gamer","description":"RGB mouse","price":199.90,"category":"Peripherals"}
                """;

        ResponseEntity<Map> created = restClient.post()
                .uri("/products")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toEntity(Map.class);

        @SuppressWarnings("unchecked")
        String id = ((Map<String, Object>) created.getBody().get("data")).get("id").toString();

        ResponseEntity<Map> response = restClient.get()
                .uri("/products/{id}", id)
                .retrieve()
                .toEntity(Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
        assertThat(data.get("name")).isEqualTo("Mouse Gamer");
    }

    @Test
    void findById_shouldReturn404_whenNotFound() {
        ResponseEntity<Map> response = restClient.get()
                .uri("/products/nonexistent-id")
                .retrieve()
                .toEntity(Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).containsKey("errors");
    }

    @Test
    void createProduct_shouldPublishCreatedEvent_toKafkaTopic() {
        var props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "test-" + UUID.randomUUID());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        try (var consumer = new KafkaConsumer<String, String>(props)) {
            consumer.subscribe(List.of("product"));
            consumer.poll(Duration.ofMillis(500));

            var body = """
                    {"name":"Kafka Event Product","description":"desc","price":50.00,"category":"Test"}
                    """;

            restClient.post().uri("/products")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();

            var records = KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(10));
            assertThat(records.count()).isGreaterThanOrEqualTo(1);
            var record = records.iterator().next();
            assertThat(record.key()).isNotNull();
            assertThat(record.value()).contains("\"event\":\"CREATED\"");
            assertThat(record.value()).contains("\"name\":\"Kafka Event Product\"");
        }
    }
}
