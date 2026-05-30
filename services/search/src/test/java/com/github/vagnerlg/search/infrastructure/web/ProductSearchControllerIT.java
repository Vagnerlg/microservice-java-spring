package com.github.vagnerlg.search.infrastructure.web;

import com.github.vagnerlg.search.TestcontainersConfiguration;
import com.github.vagnerlg.search.application.ProductSearchService;
import com.github.vagnerlg.search.domain.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class ProductSearchControllerIT {

    @LocalServerPort
    private int port;

    @Autowired
    private ProductSearchService service;

    @Autowired
    private ElasticsearchOperations operations;

    private RestClient restClient;

    @BeforeEach
    void setUp() {
        restClient = RestClient.builder()
                .baseUrl("http://localhost:" + port)
                .defaultStatusHandler(status -> true, (request, response) -> {})
                .build();

        service.index(product("1", "Teclado Mecânico", "Switch Cherry MX Red", "Peripherals"));
        service.index(product("2", "Mouse Gamer", "DPI ajustável 16000", "Peripherals"));
        service.index(product("3", "Notebook Dell", "Intel Core i7 32GB RAM", "Computers"));

        operations.indexOps(IndexCoordinates.of("products")).refresh();
    }

    @Test
    void shouldReturnProductsMatchingQuery() {
        var response = restClient.get()
                .uri("/products/search?q=teclado")
                .retrieve()
                .toEntity(Map.class);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        @SuppressWarnings("unchecked")
        List<?> content = (List<?>) response.getBody().get("content");
        assertThat(content).hasSize(1);
        @SuppressWarnings("unchecked")
        Map<String, Object> first = (Map<String, Object>) content.get(0);
        assertThat(first.get("name")).isEqualTo("Teclado Mecânico");
    }

    @Test
    void shouldFilterByCategory() {
        var response = restClient.get()
                .uri(u -> u.path("/products/search").queryParam("q", "gamer").queryParam("category", "Peripherals").build())
                .retrieve()
                .toEntity(Map.class);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        @SuppressWarnings("unchecked")
        List<?> content = (List<?>) response.getBody().get("content");
        assertThat(content).hasSize(1);
        @SuppressWarnings("unchecked")
        Map<String, Object> first = (Map<String, Object>) content.get(0);
        assertThat(first.get("name")).isEqualTo("Mouse Gamer");
    }

    @Test
    void shouldReturnEmptyWhenNoMatch() {
        var response = restClient.get()
                .uri("/products/search?q=cadeira")
                .retrieve()
                .toEntity(Map.class);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        @SuppressWarnings("unchecked")
        List<?> content = (List<?>) response.getBody().get("content");
        assertThat(content).isEmpty();
    }

    @Test
    void shouldSearchInDescription() {
        var response = restClient.get()
                .uri("/products/search?q=Cherry")
                .retrieve()
                .toEntity(Map.class);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        @SuppressWarnings("unchecked")
        List<?> content = (List<?>) response.getBody().get("content");
        assertThat(content).hasSize(1);
        @SuppressWarnings("unchecked")
        Map<String, Object> first = (Map<String, Object>) content.get(0);
        assertThat(first.get("name")).isEqualTo("Teclado Mecânico");
    }

    private Product product(String id, String name, String description, String category) {
        return new Product(id, name, description, new BigDecimal("299.90"), category, Instant.now(), Instant.now());
    }
}
