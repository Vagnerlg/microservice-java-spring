package com.github.vagnerlg.cart.infrastructure.web;

import com.github.vagnerlg.cart.MockConfiguration;
import com.github.vagnerlg.cart.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({TestcontainersConfiguration.class, MockConfiguration.class})
class CartControllerIT {

    @LocalServerPort
    private int port;

    private RestClient restClient;

    @BeforeEach
    void setUp() {
        restClient = RestClient.builder()
                .baseUrl("http://localhost:" + port)
                .defaultStatusHandler(status -> true, (request, response) -> {})
                .build();
    }

    private RestClient.RequestHeadersSpec<?> get(String uri, String userId) {
        return restClient.get().uri(uri).header("Authorization", "Bearer " + userId);
    }

    private RestClient.RequestBodySpec post(String uri, String userId) {
        return restClient.post().uri(uri)
                .header("Authorization", "Bearer " + userId)
                .contentType(MediaType.APPLICATION_JSON);
    }

    private RestClient.RequestBodySpec put(String uri, String userId) {
        return restClient.put().uri(uri)
                .header("Authorization", "Bearer " + userId)
                .contentType(MediaType.APPLICATION_JSON);
    }

    private RestClient.RequestHeadersSpec<?> delete(String uri, String userId) {
        return restClient.delete().uri(uri).header("Authorization", "Bearer " + userId);
    }

    @Test
    void getCart_shouldReturn200_withEmptyCart() {
        ResponseEntity<Map> response = get("/carts", "user-get").retrieve().toEntity(Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        var data = (Map<String, Object>) response.getBody().get("data");
        assertThat(data.get("userId")).isEqualTo("user-get");
        assertThat(data.get("items")).isInstanceOf(java.util.List.class);
    }

    @Test
    void addItem_shouldReturn200_withCartContainingItem() {
        String body = """
                {"productId":"prod-1","name":"Tênis X","price":299.90,"quantity":2}
                """;

        ResponseEntity<Map> response = post("/carts/items", "user-add")
                .body(body)
                .retrieve()
                .toEntity(Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        var data = (Map<String, Object>) response.getBody().get("data");
        @SuppressWarnings("unchecked")
        var items = (java.util.List<Map<String, Object>>) data.get("items");
        assertThat(items).hasSize(1);
        assertThat(items.get(0).get("productId")).isEqualTo("prod-1");
        assertThat(items.get(0).get("quantity")).isEqualTo(2);
    }

    @Test
    void addItem_withSameProduct_shouldSumQuantities() {
        String body = """
                {"productId":"prod-sum","name":"Tênis X","price":299.90,"quantity":2}
                """;
        post("/carts/items", "user-sum").body(body).retrieve().toBodilessEntity();

        ResponseEntity<Map> response = post("/carts/items", "user-sum")
                .body(body)
                .retrieve()
                .toEntity(Map.class);

        @SuppressWarnings("unchecked")
        var data = (Map<String, Object>) response.getBody().get("data");
        @SuppressWarnings("unchecked")
        var items = (java.util.List<Map<String, Object>>) data.get("items");
        assertThat(items).hasSize(1);
        assertThat(items.get(0).get("quantity")).isEqualTo(4);
    }

    @Test
    void updateItem_shouldReturn200_withUpdatedQuantity() {
        String addBody = """
                {"productId":"prod-upd","name":"Tênis X","price":299.90,"quantity":2}
                """;
        post("/carts/items", "user-upd").body(addBody).retrieve().toBodilessEntity();

        ResponseEntity<Map> response = put("/carts/items/prod-upd", "user-upd")
                .body("{\"quantity\":10}")
                .retrieve()
                .toEntity(Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        var items = (java.util.List<Map<String, Object>>)
                ((Map<String, Object>) response.getBody().get("data")).get("items");
        assertThat(items.get(0).get("quantity")).isEqualTo(10);
    }

    @Test
    void updateItem_withNonExistentProduct_shouldReturn404() {
        ResponseEntity<Map> response = put("/carts/items/non-existent", "user-404")
                .body("{\"quantity\":1}")
                .retrieve()
                .toEntity(Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).containsKey("errors");
    }

    @Test
    void removeItem_shouldReturn200_withoutRemovedItem() {
        String addBody = """
                {"productId":"prod-del","name":"Tênis X","price":299.90,"quantity":2}
                """;
        post("/carts/items", "user-del").body(addBody).retrieve().toBodilessEntity();

        ResponseEntity<Map> response = delete("/carts/items/prod-del", "user-del")
                .retrieve()
                .toEntity(Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        var items = (java.util.List<?>) ((Map<String, Object>) response.getBody().get("data")).get("items");
        assertThat(items).isEmpty();
    }

    @Test
    void clearCart_shouldReturn204() {
        ResponseEntity<Void> response = delete("/carts", "user-clear")
                .retrieve()
                .toBodilessEntity();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void checkout_shouldReturn204_andClearCart() {
        String addBody = """
                {"productId":"prod-co","name":"Tênis X","price":299.90,"quantity":1}
                """;
        post("/carts/items", "user-co").body(addBody).retrieve().toBodilessEntity();

        ResponseEntity<Void> checkout = post("/carts/checkout", "user-co")
                .retrieve()
                .toBodilessEntity();
        assertThat(checkout.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<Map> cartAfter = get("/carts", "user-co").retrieve().toEntity(Map.class);
        @SuppressWarnings("unchecked")
        var items = (java.util.List<?>) ((Map<String, Object>) cartAfter.getBody().get("data")).get("items");
        assertThat(items).isEmpty();
    }

    @Test
    void checkout_withEmptyCart_shouldReturn422() {
        ResponseEntity<Map> response = post("/carts/checkout", "user-empty-co")
                .retrieve()
                .toEntity(Map.class);

        assertThat(response.getStatusCode().value()).isEqualTo(422);
        assertThat(response.getBody()).containsKey("errors");
    }

    @Test
    void anyEndpoint_withoutToken_shouldReturn401() {
        ResponseEntity<Void> response = restClient.get()
                .uri("/carts")
                .retrieve()
                .toBodilessEntity();

        assertThat(response.getStatusCode().value()).isEqualTo(401);
    }

    @Test
    void addItem_withInvalidBody_shouldReturn422() {
        ResponseEntity<Map> response = post("/carts/items", "user-val")
                .body("{\"productId\":\"\",\"name\":\"\",\"price\":-1,\"quantity\":0}")
                .retrieve()
                .toEntity(Map.class);

        assertThat(response.getStatusCode().value()).isEqualTo(422);
        assertThat(response.getBody()).containsKey("errors");
    }
}
