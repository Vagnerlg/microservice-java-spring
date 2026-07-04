package com.github.vagnerlg.search.infrastructure.elasticsearch;

import co.elastic.clients.json.JsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.instrumentation.OpenTelemetryForElasticsearch;
import co.elastic.clients.transport.rest5_client.Rest5ClientTransport;
import co.elastic.clients.transport.rest5_client.low_level.Rest5Client;
import io.opentelemetry.api.OpenTelemetry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class ElasticsearchOtelConfiguration {

    @Bean
    ElasticsearchTransport elasticsearchTransport(
            Rest5Client rest5Client,
            JsonpMapper jsonpMapper,
            OpenTelemetry openTelemetry) {
        return new Rest5ClientTransport(
                rest5Client,
                jsonpMapper,
                null,
                new OpenTelemetryForElasticsearch(openTelemetry, false)
        );
    }
}
