package com.github.vagnerlg.observability;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ObservabilityEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final String DEFAULTS_RESOURCE = "observability-defaults.yml";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        List<PropertySource<?>> sources = loadDefaults();
        if (Arrays.asList(environment.getActiveProfiles()).contains("prod")) {
            Map<String, Object> prodProps = new LinkedHashMap<>();
            prodProps.put("logging.structured.format.console", "ecs");
            sources = new java.util.ArrayList<>(sources);
            sources.add(new MapPropertySource("observability-prod-defaults", prodProps));
        }
        sources.forEach(source -> environment.getPropertySources().addLast(source));

        // Resolve OTEL_ENDPOINT now (env vars are available at this point) and inject
        // concrete URLs so OtlpMeterRegistry picks them up before its auto-configuration
        // runs — without this, the Micrometer OtlpMeterRegistry falls back to its Java
        // default of localhost:4318 instead of reading the placeholder from observability-defaults.yml.
        String otelEndpoint = environment.getProperty("OTEL_ENDPOINT", "http://localhost:4318");
        Map<String, Object> otelProps = new LinkedHashMap<>();
        otelProps.put("management.otlp.metrics.export.url", otelEndpoint + "/v1/metrics");
        otelProps.put("management.opentelemetry.tracing.export.otlp.endpoint", otelEndpoint + "/v1/traces");
        otelProps.put("management.opentelemetry.logging.export.otlp.endpoint", otelEndpoint + "/v1/logs");
        environment.getPropertySources().addLast(new MapPropertySource("observability-otel-resolved", otelProps));
    }

    private List<PropertySource<?>> loadDefaults() {
        try {
            return new YamlPropertySourceLoader()
                    .load("observability-defaults", new ClassPathResource(DEFAULTS_RESOURCE));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load " + DEFAULTS_RESOURCE, e);
        }
    }
}
