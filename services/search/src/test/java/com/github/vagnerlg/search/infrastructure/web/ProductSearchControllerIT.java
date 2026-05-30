package com.github.vagnerlg.search.infrastructure.web;

import com.github.vagnerlg.search.TestcontainersConfiguration;
import com.github.vagnerlg.search.application.ProductSearchService;
import com.github.vagnerlg.search.domain.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ProductSearchControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductSearchService service;

    @Autowired
    private ElasticsearchOperations operations;

    @BeforeEach
    void setUp() {
        var indexOps = operations.indexOps(IndexCoordinates.of("products"));
        if (indexOps.exists()) {
            indexOps.delete();
        }

        service.index(product("1", "Teclado Mecânico", "Switch Cherry MX Red", "Periféricos"));
        service.index(product("2", "Mouse Gamer", "DPI ajustável 16000", "Periféricos"));
        service.index(product("3", "Notebook Dell", "Intel Core i7 32GB RAM", "Computadores"));

        operations.indexOps(IndexCoordinates.of("products")).refresh();
    }

    @Test
    void shouldReturnProductsMatchingQuery() throws Exception {
        mockMvc.perform(get("/products/search").param("q", "teclado"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name").value("Teclado Mecânico"));
    }

    @Test
    void shouldFilterByCategory() throws Exception {
        mockMvc.perform(get("/products/search")
                        .param("q", "gamer")
                        .param("category", "Periféricos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name").value("Mouse Gamer"));
    }

    @Test
    void shouldReturnEmptyWhenNoMatch() throws Exception {
        mockMvc.perform(get("/products/search").param("q", "cadeira"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));
    }

    @Test
    void shouldSearchInDescription() throws Exception {
        mockMvc.perform(get("/products/search").param("q", "Cherry"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name").value("Teclado Mecânico"));
    }

    private Product product(String id, String name, String description, String category) {
        return new Product(id, name, description, new BigDecimal("299.90"), category, Instant.now(), Instant.now());
    }
}
