package com.github.vagnerlg.search.infrastructure.elasticsearch;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MultiMatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TermQuery;
import com.github.vagnerlg.search.domain.Product;
import com.github.vagnerlg.search.domain.ProductRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.stereotype.Component;

@Component
class ElasticsearchProductRepository implements ProductRepository {

    private final ElasticsearchOperations operations;

    ElasticsearchProductRepository(ElasticsearchOperations operations) {
        this.operations = operations;
    }

    @PostConstruct
    void ensureMapping() {
        var indexOps = operations.indexOps(ProductDocument.class);
        try {
            indexOps.createWithMapping();
        } catch (Exception ignored) {
            indexOps.putMapping();
        }
    }

    @Override
    public void save(Product product) {
        operations.save(toDocument(product));
    }

    @Override
    public void deleteById(String id) {
        operations.delete(id, ProductDocument.class);
    }

    @Override
    public Page<Product> search(String query, String category, Pageable pageable) {
        var multiMatch = MultiMatchQuery.of(m -> m.query(query).fields("name", "description"))._toQuery();
        var finalQuery = buildQuery(multiMatch, category);

        var nativeQuery = NativeQuery.builder()
                .withQuery(finalQuery)
                .withPageable(pageable)
                .build();

        var hits = operations.search(nativeQuery, ProductDocument.class);
        var products = hits.getSearchHits().stream()
                .map(hit -> toDomain(hit.getContent()))
                .toList();
        return new PageImpl<>(products, pageable, hits.getTotalHits());
    }

    private Query buildQuery(Query multiMatch, String category) {
        if (category != null && !category.isBlank()) {
            return BoolQuery.of(b -> b
                    .must(multiMatch)
                    .filter(TermQuery.of(t -> t.field("category").value(category))._toQuery())
            )._toQuery();
        }
        return multiMatch;
    }

    private ProductDocument toDocument(Product p) {
        return new ProductDocument(p.id(), p.name(), p.description(), p.price(), p.category(), p.createdAt(), p.updatedAt());
    }

    private Product toDomain(ProductDocument d) {
        return new Product(d.id(), d.name(), d.description(), d.price(), d.category(), d.createdAt(), d.updatedAt());
    }
}
