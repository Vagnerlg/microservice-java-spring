package com.github.vagnerlg.order.infrastructure.web;

import com.github.vagnerlg.order.MockConfiguration;
import com.github.vagnerlg.order.TestcontainersConfiguration;
import com.github.vagnerlg.order.application.OrderService;
import com.github.vagnerlg.order.domain.CreateOrderItem;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({TestcontainersConfiguration.class, MockConfiguration.class})
class OrderControllerIT {

    @LocalServerPort
    private int port;

    @Autowired
    private OrderService orderService;

    private RestClient restClient() {
        return RestClient.builder()
                .baseUrl("http://localhost:" + port)
                .defaultStatusHandler(status -> true, (request, response) -> {})
                .build();
    }

    @Test
    void list_shouldReturn200_withOrdersForAuthenticatedUser() {
        var items = List.of(new CreateOrderItem("p-1", "Widget", new BigDecimal("10.00"), 1));
        orderService.createFromCheckout("list-user", items);

        ResponseEntity<Map> response = restClient().get()
                .uri("/orders")
                .header("Authorization", "Bearer list-user")
                .retrieve()
                .toEntity(Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
        assertThat(data).containsKey("content");
    }

    @Test
    void getById_shouldReturn200_whenOwner() {
        var items = List.of(new CreateOrderItem("p-1", "Widget", new BigDecimal("10.00"), 1));
        var order = orderService.createFromCheckout("owner-user", items);

        ResponseEntity<Map> response = restClient().get()
                .uri("/orders/{id}", order.id())
                .header("Authorization", "Bearer owner-user")
                .retrieve()
                .toEntity(Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
        assertThat(data.get("status")).isEqualTo("PENDING");
    }

    @Test
    void getById_shouldReturn403_whenNotOwner() {
        var items = List.of(new CreateOrderItem("p-1", "Widget", new BigDecimal("10.00"), 1));
        var order = orderService.createFromCheckout("owner-user-2", items);

        ResponseEntity<Map> response = restClient().get()
                .uri("/orders/{id}", order.id())
                .header("Authorization", "Bearer other-user")
                .retrieve()
                .toEntity(Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void getById_shouldReturn404_whenNotFound() {
        ResponseEntity<Map> response = restClient().get()
                .uri("/orders/{id}", UUID.randomUUID())
                .header("Authorization", "Bearer any-user")
                .retrieve()
                .toEntity(Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).containsKey("errors");
    }

    @Test
    void cancel_shouldReturn204_whenOwnerCancelsPendingOrder() {
        var items = List.of(new CreateOrderItem("p-1", "Widget", new BigDecimal("10.00"), 1));
        var order = orderService.createFromCheckout("cancel-user", items);

        ResponseEntity<Void> response = restClient().delete()
                .uri("/orders/{id}", order.id())
                .header("Authorization", "Bearer cancel-user")
                .retrieve()
                .toEntity(Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void cancel_shouldReturn403_whenNotOwner() {
        var items = List.of(new CreateOrderItem("p-1", "Widget", new BigDecimal("10.00"), 1));
        var order = orderService.createFromCheckout("cancel-owner", items);

        ResponseEntity<Map> response = restClient().delete()
                .uri("/orders/{id}", order.id())
                .header("Authorization", "Bearer another-user")
                .retrieve()
                .toEntity(Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void list_shouldReturn401_whenNoToken() {
        ResponseEntity<Map> response = restClient().get()
                .uri("/orders")
                .retrieve()
                .toEntity(Map.class);

        assertThat(response.getStatusCode().value()).isEqualTo(401);
    }
}
