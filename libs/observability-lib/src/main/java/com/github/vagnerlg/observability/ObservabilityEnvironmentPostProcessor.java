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
